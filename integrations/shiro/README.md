## Apache Shiro

[Apache Shiro](https://shiro.apache.org) is an authentication and authorization framework. This integration provides two separate bundles which do not necessarily both need to be deployed (although they integrate well together

### Shiro Authc

This bundle provides a Shiro-based authentication REST resource for your users.

Key features

* Support for authenticating users using Apache Shiro
* Cookie based user memory
* Http Session based
* Logout support


### Shiro Authz

This bundle provides Shiro-based authorization for your REST resources. This can be used with the Shiro Authc component, or Shiro authentication can be achieved using other means, such as the Shiro Servlet Filter.

* Support for injection of Shiro Security Contexts into your JAX-RS resources
* Support for Shiro authorization annotations on your JAX-RS resources 

