// Copyright 2017 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package codeu.controller;

import codeu.model.data.Conversation;
import codeu.model.data.Message;
import codeu.model.data.User;
import codeu.model.store.basic.ConversationStore;
import codeu.model.store.basic.MessageStore;
import codeu.model.store.basic.UserStore;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import java.util.regex.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document.OutputSettings;
import org.jsoup.safety.Whitelist;
/** Servlet class responsible for the chat page. */
public class ChatServlet extends HttpServlet {

  /** Store class that gives access to Conversations. */
  private ConversationStore conversationStore;

  /** Store class that gives access to Messages. */
  private MessageStore messageStore;

  /** Store class that gives access to Users. */
  private UserStore userStore;

  /** Set up state for handling chat requests. */
  @Override
  public void init() throws ServletException {
    super.init();
    setConversationStore(ConversationStore.getInstance());
    setMessageStore(MessageStore.getInstance());
    setUserStore(UserStore.getInstance());
  }

  /**
   * Sets the ConversationStore used by this servlet. This function provides a common setup method
   * for use by the test framework or the servlet's init() function.
   */
  void setConversationStore(ConversationStore conversationStore) {
    this.conversationStore = conversationStore;
  }

  /**
   * Sets the MessageStore used by this servlet. This function provides a common setup method for
   * use by the test framework or the servlet's init() function.
   */
  void setMessageStore(MessageStore messageStore) {
    this.messageStore = messageStore;
  }

  /**
   * Sets the UserStore used by this servlet. This function provides a common setup method for use
   * by the test framework or the servlet's init() function.
   */
  void setUserStore(UserStore userStore) {
    this.userStore = userStore;
  }

  public static String replaceUrls(String text){
    String newText = text;
    ArrayList<String> containedUrls = new ArrayList<String>();
    // regular expression taken from https://stackoverflow.com/questions/5713558/detect-and-extract-url-from-a-string
    // matches with ftp, http, https, and www prefixed urls
    Pattern pattern = Pattern.compile(
       "(?:^|[\\W])((ht|f)tp(s?):\\/\\/|www\\.)"
                + "(([\\w\\-]+\\.){1,}?([\\w\\-.~]+\\/?)*"
                + "[\\p{Alnum}.,%_=?&#\\-+()\\[\\]\\*$~@!:/{};']*)",
                Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
    Matcher urlMatcher = pattern.matcher(text);

    while (urlMatcher.find())
    {
        containedUrls.add(text.substring(urlMatcher.start(0),
                urlMatcher.end(0)));
    }

    // iterate backwards over containedUrls to update the text with <a></a> tags
    for (int i = containedUrls.size()-1; i >=0; i--){
      int index = text.lastIndexOf(containedUrls.get(i));
      //remove this instance of the url from the original text:
      text = text.substring(0,index) + text.substring(index + containedUrls.get(i).length());
      //update the instance of the url in the new text:
      newText = newText.substring(0,index) + "<a href='" + containedUrls.get(i) + "'>" 
              + containedUrls.get(i) + "</a>" + newText.substring(index + containedUrls.get(i).length());
    }
    return newText;
  }

  /**
   * This function fires when a user navigates to the chat page. It gets the conversation title from
   * the URL, finds the corresponding Conversation, and fetches the messages in that Conversation.
   * It then forwards to chat.jsp for rendering.
   */
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    String requestUrl = request.getRequestURI();
    String conversationTitle = requestUrl.substring("/chat/".length());

    Conversation conversation = conversationStore.getConversationWithTitle(conversationTitle);
    if (conversation == null) {
      // couldn't find conversation, redirect to conversation list
      System.out.println("Conversation was null: " + conversationTitle);
      response.sendRedirect("/conversations");
      return;
    }

    UUID conversationId = conversation.getId();

    List<Message> messages = messageStore.getMessagesInConversation(conversationId);

    request.setAttribute("conversation", conversation);
    request.setAttribute("messages", messages);
    request.getRequestDispatcher("/WEB-INF/view/chat.jsp").forward(request, response);
  }

  /**
   * This function fires when a user submits the form on the chat page. It gets the logged-in
   * username from the session, the conversation title from the URL, and the chat message from the
   * submitted form data. It creates a new Message from that data, adds it to the model, and then
   * redirects back to the chat page.
   */
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {

    String username = (String) request.getSession().getAttribute("user");
    if (username == null) {
      // user is not logged in, don't let them add a message
      response.sendRedirect("/login");
      return;
    }

    User user = userStore.getUser(username);
    if (user == null) {
      // user was not found, don't let them add a message
      response.sendRedirect("/login");
      return;
    }

    String requestUrl = request.getRequestURI();
    String conversationTitle = requestUrl.substring("/chat/".length());

    Conversation conversation = conversationStore.getConversationWithTitle(conversationTitle);
    if (conversation == null) {
      // couldn't find conversation, redirect to conversation list
      response.sendRedirect("/conversations");
      return;
    }

    String messageContent = request.getParameter("message");

    // adjusted settings for cleaning done by Jsoup
    OutputSettings settings = new OutputSettings();
    settings.prettyPrint(false);  

    // this removes all HTML tags except for text nodes (a, b, blockquote, li, ol)
    // reference for this whitelist can be found here: https://jsoup.org/apidocs/org/jsoup/safety/Whitelist.html#basic--
    Whitelist list = Whitelist.basic();
    list.addTags("div");
    list.addAttributes("div", "class");

    String cleanedMessageContent = Jsoup.clean(messageContent, "", list, settings);
    
    // this updates urls to be hyperlink references
    cleanedMessageContent = replaceUrls(cleanedMessageContent);


    Message message =
        new Message(
            UUID.randomUUID(),
            conversation.getId(),
            user.getId(),
            cleanedMessageContent,
            Instant.now());

    messageStore.addMessage(message);

    // redirect to a GET request
    response.sendRedirect("/chat/" + conversationTitle);
  }
}
