This project is licensed under the LGPL license (see COPYING.LESSER).
Some of the source files in org.autofetch.hibernate package are based on the source
code in the Hibernate project.
The source files in the org.hibernate.cfg package are almost direct copies of the
corresponding Hibernate source files with a few slight changes.
You can find the documentation at:
http://www.cs.utexas.edu/~aibrahim/autofetch

Issues

Entities / Proxies are not serialized / deserialized correctly.
org.hibernate.Hibernate#getClass will return proxy classes for entity instances.
Custom tuplizers will not work.
Does not affect stateless sessions.
Custom interceptors must be wrapped with Autofetch Interceptors.

