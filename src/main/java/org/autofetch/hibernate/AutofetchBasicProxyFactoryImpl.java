package org.autofetch.hibernate;

import static net.bytebuddy.matcher.ElementMatchers.isFinalizer;
import static net.bytebuddy.matcher.ElementMatchers.isSynthetic;
import static net.bytebuddy.matcher.ElementMatchers.isVirtual;
import static net.bytebuddy.matcher.ElementMatchers.nameStartsWith;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static net.bytebuddy.matcher.ElementMatchers.returns;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.bytecode.internal.bytebuddy.BasicProxyFactoryImpl;
import org.hibernate.bytecode.internal.bytebuddy.ByteBuddyState;
import org.hibernate.bytecode.internal.bytebuddy.PassThroughInterceptor;
import org.hibernate.bytecode.spi.BasicProxyFactory;
import org.hibernate.cfg.Environment;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.ProxyConfiguration;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.NamingStrategy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.SuperMethodCall;
import net.bytebuddy.implementation.bind.annotation.DefaultCall;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;

public class AutofetchBasicProxyFactoryImpl extends BasicProxyFactoryImpl  implements BasicProxyFactory {

	private static final Class[] NO_INTERFACES = new Class[0];
	private static final NamingStrategy PROXY_NAMING = Environment.useLegacyProxyClassnames() ? new NamingStrategy.SuffixingRandom(
			"AutofetchBasicProxy$") : new NamingStrategy.SuffixingRandom("AutofetchBasicProxy");

	private final Class proxyClass;
	private ByteBuddy bytebuddy = new ByteBuddy();

	@SuppressWarnings("unchecked")
	public AutofetchBasicProxyFactoryImpl(Class superClass, Class[] interfaces, ByteBuddyState bytebuddy1) {
		super(superClass, interfaces, bytebuddy1);
		this.proxyClass = bytebuddy
				.with(PROXY_NAMING)
				.subclass(superClass == null ? Object.class : superClass)
				.implement(interfaces == null ? NO_INTERFACES : interfaces)
				.defineField(ProxyConfiguration.INTERCEPTOR_FIELD_NAME, ProxyConfiguration.Interceptor.class,
						Visibility.PRIVATE)
				.method( isVirtual().and( not( isFinalizer() ) ) )
				.intercept( MethodDelegation.to( ProxyConfiguration.InterceptorDispatcher.class ) )
				.method( nameStartsWith( "$$" ).and( isVirtual() ))
				.intercept( SuperMethodCall.INSTANCE)
				.implement(ProxyConfiguration.class)
				.intercept(
						FieldAccessor.ofField(ProxyConfiguration.INTERCEPTOR_FIELD_NAME).withAssigner(Assigner.DEFAULT,
								Assigner.Typing.DYNAMIC)).make().load(BasicProxyFactory.class.getClassLoader())
				.getLoaded();

	}

	public Object getProxy() {
		try {
			final ProxyConfiguration proxy = (ProxyConfiguration) proxyClass.newInstance();
			proxy.$$_hibernate_set_interceptor(new PassThroughInterceptor(proxy, proxyClass.getName()));
			return proxy;
		} catch (Throwable t) {
			throw new HibernateException("Unable to instantiate proxy instance");
		}
	}

	public boolean isInstance(Object object) {
		return proxyClass.isInstance(object);
	}
}
