<?php include('includes/header.php') ?>
<div id="intro">
  <div class="jumbotron">
    <div class="container">
      <div class="row">
        <div class="col-md-12" style="position: relative">
          <div class="row">
            <div class="col-md-6 col-md-offset-1">
		<img class="img-responsive" src="images/logo.png" >
	    </div>
	    <div class="col-md-10 col-md-offset-1">		
		<p class="lead">
		 Concierge is a small-footprint implementation of the OSGi Core Specification R5 standard optimized for mobile and embedded devices.
		</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</div>    
</div>
<div id="features">
  <div class="container">
    <div class="row">
      <div class="col-md-5 feature-box">
        <img class="img-responsive" src="images/raspberry.png">
      </div>
      <div class="col-md-6">
        <p class="feature-header">OSGi for mobile and embedded devices</p>
        <p class="feature-body">Concierge brings OSGi to your mobile and embedded devices such as the Raspberry Pi and Beaglebone black. Concierge also has support for running on Android's Dalvik VM.</p>
      </div>
    </div>
    <div class="row" style="margin-top: 50px; padding-top: 50px; border-top: 1px solid white;">
      <div class="col-md-6">
        <p class="feature-header">Small footprint implementation</p>
        <p class="feature-body">With a .jar size of around 250kb, Concierge is the smallest OSGi R5 implementation around. This results in a fast startup time and an efficient service registry. Also, the framework runs on current and upcoming Java embedded profiles (e.g. Java 8 compact profile).</p>
      </div>
      <div class="col-md-5 feature-box">
        <img class="img-responsive" src="images/footprint.png">
      </div>
    </div>
    <div class="row" style="margin-top: 50px; padding-top: 50px; border-top: 1px solid white;">
      <div class="col-md-5 feature-box">
        <img class="img-responsive" src="images/osgi.png">
      </div>
      <div class="col-md-6">
        <p class="feature-header">OSGi R5</p>
        <p class="feature-body">Concierge implements the <a href="http://www.osgi.org/Release5/HomePage">OSGi R5</a> APIs. We strictly adhere the OSGi Core specification, and omit any optional services to keep our low footprint. If needed, some extra services can be installed as separate bundles.</p>
      </div>
      </div>
    </div>
  </div>
</div>
<?php include('includes/footer.php') ?>
