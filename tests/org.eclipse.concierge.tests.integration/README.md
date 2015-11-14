# Eclipse Concierge Integration Tests

This Java project contains integration tests with bundles from different projects to run on Concierge OSGi framework.


Actually there a lot of tests using a proprietary way to download and install bundle in Concierge. It is not yet integrated in regular build as it requires manual changes to get all these tests running against latest versions.

We filed a bug to check if pax-exam is another option to write such kind of integration tests.

## References

* "Use pax-exam for Concierge integration tests? ": https://bugs.eclipse.org/bugs/show_bug.cgi?id=480566
* Pax-exam documentation: https://ops4j1.jira.com/wiki/display/PAXEXAM4/Documentation
* Maven repo with pax-exam: http://mvnrepository.com/artifact/org.ops4j.pax.exam
* more information about pax-exam
  * http://veithen.github.io/alta/examples/pax-exam.html
  * http://stackoverflow.com/questions/28109434/pax-exam-4-and-multiple-maven-repositories-not-working
