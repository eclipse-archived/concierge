# Eclipse Concierge Integration Tests

This project will contain in future integration tests with bundles from different projects to run on Concierge OSGi framework.

There have been a lot of tests using a proprietary way to download and install bundle in Concierge. This will be replaced by a pax-exam based test suite. Meanwhile the old tests have been deleted up to have a clear repository.

## References

* "Use pax-exam for Concierge integration tests? ": https://github.com/eclipse/concierge/issues/14
* Pax-exam documentation: https://ops4j1.jira.com/wiki/display/PAXEXAM4/Documentation
* Maven repo with pax-exam: http://mvnrepository.com/artifact/org.ops4j.pax.exam
* more information about pax-exam
  * http://veithen.github.io/alta/examples/pax-exam.html
  * http://stackoverflow.com/questions/28109434/pax-exam-4-and-multiple-maven-repositories-not-working
