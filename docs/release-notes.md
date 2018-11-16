# Eclipse Concierge Release Notes

## Release 5.1.0 (2018-11-13)

This release ships the reference implementations for two OSGi standards: 
* OSGi R6 Compendium / Enterprise REST Management Service Specification Version 1.0
* OSGi R7 Compendium / Enterprise Cluster Information Specification Version 1.0

Further improvements:
* Binaries are available (for the first time) on Maven Central https://search.maven.org/search?q=g:org.eclipse.concierge
* Added simple EventAdmin 1.3 implementation

Bugs fixed:
* [#45](https://github.com/eclipse/concierge/issues/45) Native library support fails in some OSGi TCK Tests
* [#39](https://github.com/eclipse/concierge/issues/39) Running Concierge on CEE-J throws a NPE in this.getClass().getClassLoader().loadCLass()
* [#37](https://github.com/eclipse/concierge/issues/37) Running Concierge on CEE-J results on NPE
* [#28](https://github.com/eclipse/concierge/issues/28) installBundle with null InputStream fails
* [#9](https://github.com/eclipse/concierge/issues/9) removeBundleListener() does not work on framework bundle

## Release 5.0.0 (2015-10-29)

* Initial release (incubation)
* See https://bugs.eclipse.org/bugs/show_bug.cgi?id=479458
