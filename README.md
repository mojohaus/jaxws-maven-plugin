# MojoHaus Jaxws Maven Plugin

This is the [jaxws-maven-plugin](http://www.mojohaus.org/jaxws-maven-plugin/).
 
[![Build Status](https://travis-ci.org/mojohaus/jaxws-maven-plugin.svg?branch=master)](https://travis-ci.org/mojohaus/jaxws-maven-plugin)

## Releasing

* Make sure `gpg-agent` is running.
* Execute `mvn -B release:prepare release:perform`

For publishing the site do the following:

```
cd target/checkout
mvn verify site site:stage scm-publish:publish-scm
```

