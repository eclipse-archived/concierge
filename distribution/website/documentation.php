<?php include('includes/header.php') ?>
<br/><br/><br/>
<div class="container" style="min-height: 800px;">
  <a class="anchor" name="basic"></a>
  <h1>1. Running and embedding Concierge</h1>

  <div id="gs"></div>
  <hr/>

  <div id="cli"></div>
  <hr/>

  <div id="bnd"></div>
  <hr/>
  
  <div id="embedding"></div>

  <br/>
  <br/>
  <a class="anchor" name="options"></a>
  <h1>2. Advanced options</h1>

  <div id="options-concierge"></div>
  <hr/>

  <div id="options-osgi"></div>
  <hr/>

  <div id="bundles"></div>

  <br/>
  <br/>
  <a class="anchor" name="develop"></a>
  <h1>3. Building and contributing</h1>

  <div id="build"></div>
  <hr/>

  <div id="contribute"></div>
</div>


<script>

  function showMarkdown(url, id){
    $.ajax({
      url: url,
      type: 'GET',
      success: function(markdown) {
        var converter = new Showdown.converter();
        var ret = converter.makeHtml(markdown);
        document.getElementById(id).innerHTML = ret;
      }
    });
  }

  showMarkdown('docs/getting-started.md', 'gs');
  showMarkdown('docs/concierge-commandline.md', 'cli');
  showMarkdown('docs/concierge-bndtools.md', 'bnd');
  showMarkdown('docs/concierge-embedding.md', 'embedding');
	    
  showMarkdown('docs/options-concierge.md', 'options-concierge');
  showMarkdown('docs/options-osgi.md', 'options-osgi');
  showMarkdown('docs/concierge-bundles.md', 'bundles');
  showMarkdown('docs/release-notes.md', 'release-notes');

  showMarkdown('docs/contributor/build-concierge.md', 'build');
  showMarkdown('docs/contributor/contribute.md', 'contribute');
</script>

<?php include('includes/footer.php') ?>
