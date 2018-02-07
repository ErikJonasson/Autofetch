# Autofetch
Migration of a legacy hibernate tool that automates prefetching.

## Current State
I'm testing the changes done with a minimal Hibernate project. With that test-project, everything compiles successfully with Hibernate 3.5.0Final. I'm currently working on Hibernate 3.6 support in the dev-branch.


## Installation
Add the Maven project to your .pom-file and the autofetch .jar-file will end up in your Maven-dependencies. 

Instead of the normal configuration initialization, do the following:
```java
Configuration cfg = new AutofetchConfiguration().configure();
SessionFactory sf = cfg.buildSessionFactory();
```
Optionally: Wrap instances of Criteria:
```java
Criteria crit = new AutofetchCriteria(sess.createCriteria(Foo.class));
```
## Goals
To migrate the tool to the newest version of Hibernate. It would be convenient to have at least one version for Hibernate 4.x aswell. 

## Documentation
The original documentation is included in the repo.

## How can you contribute?
Make a pull request, I need all the help I can get :)