# Autofetch
Migration of a legacy hibernate tool that automates prefetching.

## Current State
There is currently development on version 4.3.10Final of Hibernate in parallel with the latest release, 5.3.0.Final.

## Installation
Add the Maven project to your .pom-file and the autofetch .jar-file will end up in your Maven-dependencies. 

In cases when the AutofetchService cannot be found automatically, simply add the autofetch integrator manually:
```java
BootstrapServiceRegistry bootstrapServiceRegistry = new BootstrapServiceRegistryBuilder()
        .with( new AutofetchIntegrator())
        .build();
```
Optionally: Wrap instances of Criteria:
```java
Criteria crit = new AutofetchCriteria(sess.createCriteria(Foo.class));
```
## Goals
To have a working version for every new version of Hibernate.
## Documentation
As for right now we don't have any real documentation and refer to the Hibernate documentation.
The original documentation of the tool is included in the repo, however this is not updated. 

## How can you contribute?
Make a pull request, we appreciate all the help we can get. At this point, we still haven't solved all the problems with the proxying and lazy loading, making some
of the tests to fail.