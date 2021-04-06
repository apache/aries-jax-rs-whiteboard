## org.apache.aries.jax.rs-1.1.0

Carlos Sierra Andrés (3):
  - 99cf69e Update versions
  - 2cc51a9 Fix incorrect license
  - e67ccc6 Added documentation for OpenAPI and Jackson integration

GitHub (1):
  - 5219359 don't fail if Java Earler Access (ea) build job fails

Raymond Auge (13):
  - d4e36e8 add dependabot
  - 825b1d1 add bnd-run-maven-plugin plugin
  - 597168b update gogo dependency
  - ee74c89 [ARIES-2026] Update/unify integrations into the main build
  - 2f40ceb update jdks tested via github actions
  - 9b24fb9 update resolution results
  - 624222c add flattened pom plugin
  - c779884 quiet host bundle warning
  - c53b142 exclude flatten from eclipse execution
  - 86e0044 add bnd snapshot repository for testing
  - cf4094c add a changelog file
  - 3277bb8 prepare next release
  - 0fbbe88 [maven-release-plugin] prepare release org.apache.aries.jax.rs-1.1.0

Raymond Augé (12):
  - 40487a9 Bump jackson-jaxrs-json-provider from 2.9.6 to 2.12.0
  - 18b8b9b Bump maven-jar-plugin from 3.0.1 to 3.2.0
  - 749433c Bump findbugs-maven-plugin from 2.4.0 to 3.0.5
  - f13194c Bump osgi.annotation from 7.0.0 to 8.0.0
  - 2a16400 Bump geronimo-annotation_1.3_spec from 1.1 to 1.3
  - 4aa173e only build for dependabot PRs not branches
  - 33a2e38 Bump jettison from 1.3.8 to 1.4.1
  - 3a97ec2 Bump shiro.version from 1.4.0 to 1.7.0
  - 1598a2d Bump org.apache.felix.scr from 2.1.16 to 2.1.24
  - 1b35737 Bump commons-lang3 from 3.10 to 3.11
  - 888072b Bump org.osgi.util.promise from 1.1.0 to 1.1.1
  - 222cc00 remove commons configuration since it's not used

jbonofre (1):
  - 1c374b0 [maven-release-plugin] prepare for next development iteration

## org.apache.aries.jax.rs-1.0.10

Carlos Sierra Andrés (8):
  - e8c4656 [ARIES-JAXRS-Whiteboard][maven-release-plugin] 1.0.9 prepare for next development iteration
  - e5321e7 [ARIES-1996] Also set runtime delegate when starting the bundle
  - eafb944 [ARIES-2001] Add test to assert the service is released just after introspection
  - e50fc8f [ARIES-2001] Refactor to not use ServiceTuple after getResourceProvider
  - 58d3f09 [ARIES-2001] Dispose the serviceTuple as soon as the registration is complete
  - 63bcd26 obtain the instance lazily
  - 5430a3f Fix indentation
  - eda6d6b make it more obvious it is one or the other

Raymond Auge (1):
  - 25e4f12 add missing example and update the README

Romain Manni-Bucau (5):
  - 9bc35a6 [ARIES-2002] ensure proxies are unwrapped for jaxrs resources
  - a1d59be [ARIES-2002] test for unproxying of getSingletons
  - db1cb1b [ARIES-2003] ensure @ApplicationPath is always ignored
  - eb41442 [ARIES-2002][ARIES-2003] IT for auto unproxying and @ApplicationPath ignore logic
  - 9f7d741 [ARIES-2002] better IT test for unproxying, thanks csierra for the catch

jbonofre (1):
  - 6dd1a3a [maven-release-plugin] prepare release org.apache.aries.jax.rs-1.0.10

## org.apache.aries.jax.rs-1.0.9

Carlos Sierra Andrés (12):
  - b04385c [ARIES-JAXRS-Whiteboard][maven-release-plugin] 1.0.8 prepare for next development iteration
  - 35fab08 [ARIES-1968] Use latest CXF
  - 129da98 Document osgi.http.whiteboard.context.select default
  - 3bb9f4d `servlet.init.` prefix needs to be specified
  - df509e9 Document application.ready.service.filter
  - ce7c3d6 Document that the properties are copied to the registered services
  - c42e8bd Document default.application.base
  - 3f05c3c make sure to use the right version of itests-fragment
  - 171d928 [ARIES-1871] Wait for messages to arrive
  - 7828469 Use latest version
  - f2c5187 Prepare for next release
  - 868050b [ARIES-JAXRS-Whiteboard][maven-release-plugin] 1.0.9 prepare release org.apache.aries.jax.rs-1.0.9

Christian Schneider (1):
  - 7be5ee8 Document configuration properties (thanks to Ray)

GitHub (1):
  - 0972de3 add actions badge

Markus Rathgeb (1):
  - fa67be9 [ARIES-1974] do not replace loopback addresses

Raymond Auge (20):
  - 14b8aca [ARIES-1968] use released version
  - f77e6c4 remove unused legacy resources
  - 333f37e some project cleanup
  - 685d9c3 [ARIES-1980] Move getResourceProvider to Whiteboard
  - f321c13 [ARIES-1980] make it return OSGi to participate in the lifecycle
  - e5521f3 [ARIES-1980] Add org.apache.aries.jax.rs.whiteboard.application.scoped property
  - 78bb981 [ARIES-1980] Update tests
  - c309206 [ARIES-1983] openapi integration
  - c0bebf9 github actions
  - 43788d6 [ARIES-1871] Wait for messages to arrive
  - e199656 make project name and description more consistent
  - 9b99c10 let Java version be determined at runtime to support building and testing with different Java versions
  - af63876 in order to keep diffs clean always keep -runbundles in order (we can use -runstartlevel if specific ordering is ever required)
  - 5e0cd50 update to spifly 1.3.0
  - aeb9ba5 prepare to support versions of Java that do not contain JAXB
  - ceb7071 add Java 11 & 14
  - a9384d5 update to bnd 5.1.0
  - c8ac469 test against Java 15-ea
  - 87ef144 add exports required for java 15
  - 7f16404 fix cxf java 15 detection bug

## org.apache.aries.jax.rs-1.0.8

Carlos Sierra Andrés (8):
  - 281f15a [ARIES-JAXRS-Whiteboard][maven-release-plugin] 1.0.7 prepare for next development iteration
  - c9c00e0 [ARIES-1931] Remove defaultweb
  - 625a115 [ARIES-1964] Hide /services CXF endpoint by default
  - 79ee095 Upgrade to CXF 3.2.12
  - e4d2dee [ARIES-1968] Use latest CXF
  - dee3b4d Revert "[ARIES-1968] Use latest CXF"
  - 0608e52 Check in resolver output
  - 065e22c [ARIES-JAXRS-Whiteboard][maven-release-plugin] 1.0.8 prepare release org.apache.aries.jax.rs-1.0.8

## org.apache.aries.jax.rs-1.0.7

Carlos Sierra (1):
  - 20c8a0a [ARIES-JAXRS-Whitebord][maven-release-plugin] 1.0.6 prepare for next development iteration

Carlos Sierra Andrés (6):
  - f98319d Do not override default HTTP Whiteboard context
  - a6a383e Fully initialize ServletContextHelper
  - f0417ba [ARIES-1962] Proper registration order
  - 9de6f51 [ARIES-1962] Generalize
  - 659d1b3 [ARIES-1963] Use concurrent set
  - 6fc1e78 [ARIES-JAXRS-Whiteboard][maven-release-plugin] 1.0.7 prepare release org.apache.aries.jax.rs-1.0.7

Tim Ward (1):
  - 22a5cf6 Add tests showing that Aries JAX-RS can break static resource handling

## org.apache.aries.jax.rs-1.0.6

Carlos Sierra (17):
  - c7ce0a6 [ARIES-JAX-RS-whiteboard][maven-release-plugin] 1.0.5 prepare for next development iteration
  - a49649e [ARIES-1929] Defer jaxrs registration initialization
  - 0816520 [ARIES-1929] Special error handling in the first rewire
  - 5e16be2 [ARIES-1929] Add test for errors in the initial case
  - bdd962c [ARIES-1929] Add support for a ready service for applications
  - bd50412 [ARIES-1927] Change order of checks
  - 81d53f0 This test was wrong
  - b147aba Source cleanup
  - b214935 [ARIES-1928] Track extensions per application
  - 7198504 [ARIES-1928] No need to register the registrators in OSGi registry
  - 978eca4 [ARIES-1928] Add new test
  - c48990b Update versions
  - 14d481f [ARIES-1928] These are still needed
  - e24cbef Update versions
  - 3577c3f Use latest release for component DSL
  - c437a6a Commit resolver output
  - 75f978b [ARIES-JAXRS-Whitebord][maven-release-plugin] 1.0.6 prepare release org.apache.aries.jax.rs-1.0.6

Raymond Auge (1):
  - c607c3f build badges

## org.apache.aries.jax.rs-1.0.5

Carlos Sierra (11):
  - e3bac54 [ARIES-JAX-RS-Whiteboard][maven-release-plugin] 1.0.4 prepare for next development iteration
  - 7c34c92 [ARIES-JAX-RS-integrations][maven-release-plugin] 1.0.2 prepare release org.apache.aries.jax.rs.integration-1.0.2
  - 73ef94b [ARIES-JAX-RS-integrations][maven-release-plugin] 1.0.2 prepare for next development iteration
  - b578664 [ARIES-1914] Store extension resolution state per application
  - 284ecf5 Prevent class loader leak
  - c523979 Add test for dependent extensions
  - 0aa5a0e [ARIES-1916] Fix request lifecycle
  - f97bfc6 add missing uses contraints
  - 05a965b Exclude newly detected package
  - daed160 [ARIES-1916] Add test
  - e3bbce0 [ARIES-JAX-RS-whiteboard][maven-release-plugin] 1.0.5 prepare release org.apache.aries.jax.rs-1.0.5

Carlos Sierra Andrés (1):
  - 4a473ea Flatten the CXF and Aries dependencies into the Whiteboard JAR

Raymond Auge (4):
  - 053927e bnd 4.2.0 and resolution
  - 6a207ba [itest.fragment] use permantent version (just used the latest version)
  - c1195de [eclipse] JDT just does not like this method usage so give in so that it will compile in eclipse
  - 64c2461 [tidy up] bnd 4.2.0, use bundle annotations where possible, etc.

## org.apache.aries.jax.rs-1.0.4

Carlos Sierra (19):
  - d9d0081 [ARIES-JAX-RS-WHITEBOARD][maven-release-plugin] 1.0.3 prepare for next development iteration
  - 2c2a4a1 Auto update bndrun
  - cd9befd Use release
  - 5a500d6 [ARIES-JAX-RS-WHITEBOARD-INTEGRATIONS][maven-release-plugin] 1.0.1 prepare release org.apache.aries.jax.rs.whiteboard.integrations-1.0.1
  - 9b54d5e [ARIES-JAX-RS-WHITEBOARD-INTEGRATIONS][maven-release-plugin] 1.0.1 prepare for next development iteration
  - c7745c4 Revert "[ARIES-JAX-RS-WHITEBOARD-INTEGRATIONS][maven-release-plugin] 1.0.1 prepare for next development iteration"
  - a5788a6 Revert "[ARIES-JAX-RS-WHITEBOARD-INTEGRATIONS][maven-release-plugin] 1.0.1 prepare release org.apache.aries.jax.rs.whiteboard.integrations-1.0.1"
  - 3e2fba4 mark as deployable
  - 100e41f [ARIES-JAX-RS-WHITEBOARD-INTEGRATIONS][maven-release-plugin] 1.0.1 prepare release org.apache.aries.jax.rs.whiteboard.integrations-1.0.1
  - b65e7ac [ARIES-JAX-RS-WHITEBOARD-INTEGRATIONS][maven-release-plugin] 1.0.1 prepare for next development iteration
  - cfead5b [ARIES-1884] Only register to default application by default
  - d455961 Rearrange code
  - 3aff199 Test application dependencies against the runtime
  - 7aaeb82 [ARIES-1888] Use SingletonResourceProvider
  - e9e646f [ARIES-1892] Avoid NPE
  - 5300d16 [ARIES-1892] Avoid NPE
  - 6bfef5f [ARIES-1893] Fill the serviceDTO
  - 3a21658 [ARIES-1899] Unify configurations
  - 2805ad3 [ARIES-JAX-RS-Whiteboard][maven-release-plugin] 1.0.4 prepare release org.apache.aries.jax.rs-1.0.4

## org.apache.aries.jax.rs-1.0.3

Carlos Sierra (11):
  - d520d4e [ARIES-JAX-RS Whiteboard][maven-release-plugin] 1.0.2prepare for next development iteration
  - 7772301 Update automatic resolution itest.bndrun
  - 66a9eaa Upgrade SCM information
  - 976ab8a [ARIES-JAX-RS Whiteboard integrations][maven-release-plugin] 1.0.0 prepare release org.apache.aries.jax.rs.whiteboard.integrations-1.0.0
  - e55df2e [ARIES-JAX-RS Whiteboard integrations][maven-release-plugin] 1.0.0 prepare for next development iteration
  - 53cba28 [ARIES-1874] Use "/" as path for default ServletContextHelper
  - e9db12a Move properties to common parent
  - 04c17f1 Code cleaning
  - 937ce8a Fix license header
  - fd4d7de Auto update bndrun
  - d7b93e9 [ARIES-JAX-RS-WHITEBOARD][maven-release-plugin] 1.0.3 prepare release org.apache.aries.jax.rs-1.0.3

Jean-Baptiste Onofré (3):
  - d45f76c [ARIES-1875] Use http-whiteboard feature in the aries-jax-rs-whiteboard one
  - fb372d9 [ARIES-1876] Add aries-jax-rs-whiteboard-jackson feature
  - 0c320db Use jackson integration 1.0.0 (currently on stage) in the features

Raymond Auge (1):
  - afff1b7 project and whitespace cleanup, eliminate other warnings (no logical changes at all)

## org.apache.aries.jax.rs-1.0.2

Carlos Sierra (16):
  - 47506ef [ARIES-JAXRS-Whiteabord][maven-release-plugin] 1.0.1prepare for next development iteration
  - 663414e [ARIES-1843] Check if any service is unregistering
  - 843fd7a [ARIES-1842] Handle null returns
  - 945d48e Update exported packages for the tests
  - 16f3c9f [ARIES-1852] Missing null check
  - 50d7e9d test avoid a NPE when a method return type is void
  - dcc2cb5 [ARIES-1865] Avoid double unregister
  - 71d88fe [ARIES-1866] Add tests showing tie problem
  - 21ab761 [ARIES-1866] Add comparator opt-in comparator
  - 6a553b4 [ARIES-1868] Properly report if the service is singleton
  - 215e382 [ARIES-1867] Add tests showing filter behaviour
  - ad8d939 Update test resolution
  - 30042ac [ARIES-1870] Do not use file system dependent classes
  - 03ef719 [ARIES-1866] Delegate to JAX-RS ordering
  - 68672b5 Recover CXF JAXB Jettison support under integrations
  - aa79972 [ARIES-JAX-RS Whiteboard][maven-release-plugin] 1.0.2prepare release org.apache.aries.jax.rs-1.0.2

Jean-Baptiste Onofré (1):
  - 32eb367 [ARIES-1872] Add Aries JAX-RS Whiteboard Karaf features repository

Raymond Auge (7):
  - 01e6e29 unify pom License header formatting
  - aa4df81 unify pom spacing
  - e1779fb prevent eclipse m2e build from complaining
  - cd133e0 ARIES-1833 Embed component-dsl into jackson extension
  - 079ebef ARIES-1834 add missing osgi.jaxrs implementation requirement to jackson extension
  - 11eb9ce prevent eclipse m2e build from complaining
  - 9face0a re-resolve

## org.apache.aries.jax.rs-1.0.1

Carlos Sierra (12):
  - e20e691 [ARIES-JAXRS-Whiteboard][maven-release-plugin] 1.0.0 prepare for next development iteration
  - 85a5b12 Update version resolution
  - d5e624e Update version resolution
  - 96ce3a2 [ARIES-1821] Use prototype scope
  - 3ffc072 Apply count as the latest effect
  - dc976c1 Avoid concurrent modification exception
  - 9f7ecf1 Use (fixed) accumulate
  - 0d7503c [ARIES-1827] Add application.base.prefix to whiteboard
  - 2cbdcdf Do not register anything until start is called
  - 7bd229e Use latest released version
  - 08ceae1 Update SCM information
  - 9bb3709 [ARIES-JAXRS-Whiteabord][maven-release-plugin] 1.0.1prepare release org.apache.aries.jax.rs-1.0.1

Raymond Auge (8):
  - dd39b08 remove unused constant
  - a88136a schemagen is not actually needed
  - f29dcb5 some cleanup
  - 556bf64 setup logback/log1.4 logging
  - 507451a ARIES-1823 Optionally support Service Loader Mediator
  - 6eafab4 ARIES-1824 Add missing `osgi.jaxrs.media.type` properties to extensions
  - 9739cf5 delete ununsed fragment module
  - 1eb36e0 Revert "delete ununsed fragment module"

Tim Ward (6):
  - 16da112 Add an integration for Apache Shiro to the JAX-RS Whiteboard
  - 7ceb8f1 Use the Jackson JSON support for JAX-RS, and make it an integration project
  - 8164371 Add support for OSGi Promises as natively asynchronous return types from resource methods
  - 2de73ab Add Provide-Capabilities advertising the integration services, including fixed properties for identification
  - 9bd8e00 Fix typo in the Provide-Capability service definitions
  - 74d37d4 [ARIES-1825] Obtain registrator earlier
