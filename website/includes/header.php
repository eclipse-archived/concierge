<!DOCTYPE html>
<html lang="en">
<head>
	<meta charset="utf-8">
	<title>Concierge - A small-footprint implementation of the OSGi Core R5 Specification</title>
	<meta name="viewport" content="width=device-width, initial-scale=1.0">
	<meta name="description" content="Concierge - A small-footprint implementation of the OSGi Core R5 Specification">
	<meta name="author" content="Tim Verbelen">
	<!--  styles -->
	<!-- Le HTML5 shim, for IE6-8 support of HTML5 elements -->
	<!--[if lt IE 9]>
	  <script src="http://html5shim.googlecode.com/svn/trunk/html5.js"></script>
	<![endif]-->

	<!-- Custom Fonts -->
    	<link href='https://fonts.googleapis.com/css?family=Lato:300,400' rel='stylesheet' type='text/css'>
    	<link href='https://fonts.googleapis.com/css?family=Lekton:400,700' rel='stylesheet' type='text/css'><!-- Styles -->
   	<link href="https://netdna.bootstrapcdn.com/font-awesome/4.0.3/css/font-awesome.css" rel="stylesheet">
	
	<link rel="shortcut icon" href="images/icon.png">

	<link href="//maxcdn.bootstrapcdn.com/bootstrap/3.2.0/css/bootstrap.min.css" rel="stylesheet" type='text/css'>
	<link href="css/style.css" rel="stylesheet" type="text/css">

	<!-- jQuery and Bootstrap --> 
	<script src="https://ajax.googleapis.com/ajax/libs/jquery/2.1.1/jquery.min.js"></script>
	<script src="//maxcdn.bootstrapcdn.com/bootstrap/3.2.0/js/bootstrap.min.js"></script>

	<!-- Showdown Markdown converter --> 
	<script src="//cdnjs.cloudflare.com/ajax/libs/showdown/0.3.1/showdown.min.js"></script>
</head>
<body>
	<!-- Navbar -->
	<nav class="navbar navbar-fixed-top"
		style="border-bottom: 1px solid #000;">
		<div class="container">
			<div class="navbar-header">
				<a type="button" class="navbar-toggle" data-toggle="collapse" data-target=".navbar-collapse">
	        		<span class="sr-only">Toggle navigation</span>
	        		<span class="icon-bar"></span>
	        		<span class="icon-bar"></span>
	        		<span class="icon-bar"></span>
	     		 </a>
      			<a class="navbar-brand" href="index.php">Concierge</a>
			</div>
			<!-- Collect the nav links, forms, and other content for toggling -->
    			<div class="collapse navbar-collapse">
    			<ul class="nav navbar-nav">
			        <li><a href="https://projects.eclipse.org/projects/rt.concierge/downloads" target="_blank">Downloads</a></li>
			        <li class="dropdown">
			            <a href="documentation.php" class="dropdown-toggle" data-toggle="dropdown">Documentation <b class="caret"></b></a>
			            <ul class="dropdown-menu">
				      <li><a href="documentation.php#basic">Getting Started</a></li>
				      <li><a href="documentation.php#options">Advanced options</a></li>
				      <li><a href="documentation.php#develop">Building and contributing</a></li>
			            </ul>
			        </li>
			        <li class="dropdown">
			          <a href="#" class="dropdown-toggle" data-toggle="dropdown">Community <b class="caret"></b></a>
			         <ul class="dropdown-menu">
			           	<li><a href="https://dev.eclipse.org/mailman/listinfo/concierge-dev" target="_blank">Mailing List</a></li>
			           	<li><a href="https://github.com/eclipse/concierge/issues" target="_blank">Issue Tracker</a></li>
					<li><a href="https://github.com/eclipse/concierge" target="_blank">Source Code</a></li>
					<li><a href="https://hudson.eclipse.org/concierge" target="_blank">Continuous Integration</a></li>
			         </ul>
			        </li>
                	</ul>
    			<ul class="nav navbar-nav navbar-right">
				<!-- TODO use IoT logo instead? -->
    				<li style="font-size:0.8em;color:#EFF7F0;">Concierge is an <a style="display: inline-block; padding-left: 0px; padding-right: 0px; color: #009615" href="http://iot.eclipse.org">iot.eclipse.org</a> project&nbsp;&nbsp;</li>
                    		<li>
					<div align="center"><a href="http://eclipse.org/projects/what-is-incubation.php"><img 
						align="center" src="images/egg-incubation.png" 
						border="0" alt="Incubation" height="70px"/></a></div>
                   		</li>
    			</ul>
    		</div>
		</div>
	</nav>
	<!-- Navbar End -->
<!--Container-->
