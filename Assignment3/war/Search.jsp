<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@page import="java.util.ArrayList"%>
<%@page import="classes.DataStore"%>



<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<link href="Style.css" rel="stylesheet" type="text/css" />
<title>Twitter API</title>

</head>


<body bgcolor="#EDFBFE">



<br>
<br>
<br>
<br>
<br>
<form action="GoogleAppEngine" theme="simple"  name="readReviewsForm" method="POST">
<table class="tableborder1" align="center" height="100">
	<tr>
		<th class="topheaderbkg" colspan="4">
		<center>Search </center>
		</th>
	</tr>
	<tr>
		<td><label>Name </label></td>
		<td><input type="text" name="searchText"/></td>
	</tr>
	<tr>
		<td colspan="4">
		<table align="center">
			<tr>
				<td><input type="submit" name="Search" value="Search" align="center"
					class="inputbutton1" /></td>
				<td><input type="reset" value="Reset" align="center"
					class="inputbutton1" /></td>
			</tr>
		</table>
		</td>
	</tr>
</table>

</br>
</br>
</br>
<%

Boolean isResultSet = (Boolean)session.getAttribute("isResultSet");
if(isResultSet!=null)
{
%>
<table border="1" class="tableborder1" align="center" width="50%">
	<tr>
		<th class="topheaderbkg1" colspan="4">Review Details</th>
	</tr>
	<tr>
		<th class="topheaderbkg1" width=10%><nobr>User Image</nobr></th>
		<th class="topheaderbkg1" width=10%><nobr>User Name</nobr></th>
		<th class="topheaderbkg1" width=10%><nobr>Tweet</nobr></th>
		<th class="topheaderbkg1" width=10%><nobr>Tweet Time</nobr></th>
	</tr>
	<%
	ArrayList<DataStore> listOfTweets=(ArrayList<DataStore>)session.getAttribute("listOfTweets");
	System.out.println(listOfTweets.size());
	DataStore tweet;
	for(int i=0;i<listOfTweets.size();i++)
	{
		tweet = (DataStore)listOfTweets.get(i);
	%>
	<tr>
		<td  ><nobr><image src=<%=tweet.getUserImage()%>/></nobr></th>
		<td  ><nobr><%=tweet.getUserName() %></nobr></th>
		<td  ><nobr><%=tweet.getTweetText() %></nobr></th>
		<td  ><nobr><%=tweet.getCreatedTime() %></nobr></th>
	</tr>
	<%
	}
	%>
</table>
<%
session.setAttribute("isResultSet", null);
}
%>
</form>
		

</body>

</html>
