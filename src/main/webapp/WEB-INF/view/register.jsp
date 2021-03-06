<%--
  Copyright 2017 Google Inc.

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
--%>
<%@ page import="java.util.Collections" %>
<%@ page import="java.util.Comparator" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="com.google.auth.Credentials" %>
<%@ page import="com.google.auth.oauth2.GoogleCredentials" %>
<%@ page import="com.google.cloud.translate.Translate" %>
<%@ page import="com.google.cloud.translate.Translate.TranslateOption" %>
<%@ page import="com.google.cloud.translate.Translate.LanguageListOption" %>
<%@ page import="com.google.cloud.translate.TranslateOptions" %>
<%@ page import="com.google.cloud.translate.Translation" %>
<%@ page import="com.google.cloud.translate.Language" %>

<%
// Puposely left in a compiler error to remind us to set this before deployment.
// Change this before deploying, but don't check this into GitHub!
String APIKEY = YOUR_API_KEY_HERE;
Translate translate = TranslateOptions.newBuilder().setApiKey(APIKEY).build().getService();
LanguageListOption target = LanguageListOption.targetLanguage("en");
List<Language> listlanguages = translate.listSupportedLanguages(target);
ArrayList<Language> languages = new ArrayList<Language>(listlanguages);
Collections.sort(languages, new Comparator<Language>(){
  public int compare(Language languageOne, Language languageTwo){
    return languageOne.getCode().compareTo(languageTwo.getCode());
  }
});
%>

<!DOCTYPE html>
<html>
<head>
  <title>Register</title>
  <link rel="stylesheet" href="/css/main.css">
  <link rel="stylesheet" href="/css/form.css">

  <style>
    @import url('https://fonts.googleapis.com/css?family=Zilla+Slab');
    </style>
</head>
<body>

  <nav>
    <a id="navTitle" href="/">CodeU Chat App</a>
    <% if(request.getSession().getAttribute("user") != null){ %>
      <a>Hello <%= request.getSession().getAttribute("user") %>!</a>
      <a href="/conversations">Conversations</a>
      <a href="/profiles">Profiles</a>
      <a href="/activityfeed">Activity</a>
      <%if (request.getSession().getAttribute("user").equals("EmilyArroyo")
      || request.getSession().getAttribute("user").equals("AlexandriaStorm")
      || request.getSession().getAttribute("user").equals("AnaVitoriadoValleCosta")
      || request.getSession().getAttribute("user").equals("KevinWorkman")
      || request.getSession().getAttribute("user").equals("GavinLifrieri")) { %>
       <a href="/admin">Administration</a>
        <% } else{ %>
          <a href="/login">Log Out<</a>
        <% } %>
           <% } else{ %>
      <a href="/login">Login</a>
    <% } %>
    <a href="/about.jsp">About</a>
  </nav>
  <div id="container">

    <% if(request.getAttribute("error") != null){ %>
        <h2 style="color:red" id="error"><%= request.getAttribute("error") %></h2>
    <% } %>


    <form action="/register" method="POST" id="form">

      <div class="form-box">
        <h2>Register</h2>

        <div class="fb-item">
          <input type="text" name="username" id="username" placeholder="Username">
          <br/>
          <span>Username</span>
        </div>

        <div class="fb-item">
          <input type="password" name="password" id="password" placeholder="Password">
          <br/>
          <span>Password</span>
        </div>

        <div class="fb-item">

            <p>What language do you want to use?</p>
            <p>Select the <a href="https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes">language code</a>
               that corresponds to your language. For example, English is en, Spanish is es, and Portuguese
               is pt.</p>
            <p>Try registering with two users in different languages to see the translation in action!</p>

            <select id="mySelect" name="language" id="language">
              <%  for (int i = 0; i < languages.size(); i++) {
                Language language = languages.get(i); %>
                <option> <%= language.getCode() %></option>
              <% } %>
            </select>

          <input type="bio" name="bio" id="bio" placeholder="Biography">
          <br/>
          <span>Please enter your biography here:</span>
        </div>

        <button type="submit" class="form-button">Register</button>
      </div>

    </form>

  </div>

</body>
</html>
