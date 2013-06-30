package br.com.caelum.vraptor4.interceptor;

import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import br.com.caelum.vraptor.core.InterceptorStack;
import br.com.caelum.vraptor.interceptor.InstanceContainer;
import br.com.caelum.vraptor4.controller.ControllerInstance;
import br.com.caelum.vraptor4.controller.ControllerMethod;
import br.com.caelum.vraptor4.controller.DefaultBeanClass;
import br.com.caelum.vraptor4.controller.DefaultControllerInstance;
import br.com.caelum.vraptor4.controller.DefaultControllerMethod;
import br.com.caelum.vraptor4.interceptor.example.AcceptsInterceptor;
import br.com.caelum.vraptor4.interceptor.example.AcceptsInterceptorWithStackAsParameter;
import br.com.caelum.vraptor4.interceptor.example.AcceptsWithoutArgsInterceptor;
import br.com.caelum.vraptor4.interceptor.example.AlwaysAcceptsAspectInterceptor;
import br.com.caelum.vraptor4.interceptor.example.AroundInterceptorWithoutSimpleStackParameter;
import br.com.caelum.vraptor4.interceptor.example.BeforeAfterInterceptorWithStackAsParameter;
import br.com.caelum.vraptor4.interceptor.example.ExampleOfSimpleStackInterceptor;
import br.com.caelum.vraptor4.interceptor.example.InterceptorWithCustomizedAccepts;
import br.com.caelum.vraptor4.interceptor.example.NonBooleanAcceptsInterceptor;
import br.com.caelum.vraptor4.interceptor.example.NotLoggedExampleController;
import br.com.caelum.vraptor4.interceptor.example.VoidAcceptsInterceptor;
import br.com.caelum.vraptor4.interceptor.example.WithoutAroundInterceptor;
import br.com.caelum.vraptor4.interceptor.example.WithoutAroundInvokeInterceptor;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AspectStyleInterceptorHandlerTest {

	private StepInvoker stepInvoker = new StepInvoker();
	private @Mock
	InterceptorStack stack;
	private @Mock
	ControllerMethod controllerMethod;
	private @Mock
	Object currentController;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void shouldAlwaysCallAround() {
		AlwaysAcceptsAspectInterceptor interceptor = spy(new AlwaysAcceptsAspectInterceptor());
		AspectStyleInterceptorHandler handler = new AspectStyleInterceptorHandler(
				AlwaysAcceptsAspectInterceptor.class, stepInvoker,
				new InstanceContainer(interceptor));
		handler.execute(stack, controllerMethod, currentController);
		verify(interceptor).intercept(Mockito.any(InterceptorStack.class),
				Mockito.same(controllerMethod),
				Mockito.any(ControllerInstance.class));
	}

	@Test
	public void shouldNotInvokeMethodIfDoesNotHaveAroundInvoke() {
		WithoutAroundInvokeInterceptor interceptor = spy(new WithoutAroundInvokeInterceptor());
		AspectStyleInterceptorHandler handler = new AspectStyleInterceptorHandler(
				WithoutAroundInvokeInterceptor.class, stepInvoker,
				new InstanceContainer(interceptor));
		handler.execute(stack, controllerMethod, currentController);
		verify(interceptor, never()).intercept(Mockito.same(stack),
				Mockito.same(controllerMethod),
				Mockito.any(ControllerInstance.class));
	}

	@Test
	public void shouldInvokeUsingBeforeAndAfter() {
		AlwaysAcceptsAspectInterceptor interceptor = spy(new AlwaysAcceptsAspectInterceptor());
		AspectStyleInterceptorHandler handler = new AspectStyleInterceptorHandler(
				AlwaysAcceptsAspectInterceptor.class, stepInvoker,
				new InstanceContainer(interceptor));
		handler.execute(stack, controllerMethod, currentController);
		InOrder order = inOrder(interceptor);
		order.verify(interceptor).begin();
		order.verify(interceptor).intercept(
				Mockito.any(InterceptorStack.class),
				Mockito.same(controllerMethod),
				Mockito.any(ControllerInstance.class));
		order.verify(interceptor).after();
	}

	@Test
	public void shouldInvokeIfAccepts() {
		AcceptsInterceptor acceptsInterceptor = spy(new AcceptsInterceptor(true));
		AspectStyleInterceptorHandler aspectHandler = new AspectStyleInterceptorHandler(
				AcceptsInterceptor.class, stepInvoker, new InstanceContainer(
						acceptsInterceptor));
		aspectHandler.execute(stack, controllerMethod, currentController);
		InOrder order = inOrder(acceptsInterceptor);
		order.verify(acceptsInterceptor).accepts(controllerMethod);
		order.verify(acceptsInterceptor).before();
		order.verify(acceptsInterceptor).around(
				Mockito.any(InterceptorStack.class),
				Mockito.same(controllerMethod),
				Mockito.any(ControllerInstance.class));
		order.verify(acceptsInterceptor).after();
	}

	@Test
	public void shouldNotInvokeIfDoesNotAccept() {
		AcceptsInterceptor acceptsInterceptor = spy(new AcceptsInterceptor(
				false));
		AspectStyleInterceptorHandler aspectHandler = new AspectStyleInterceptorHandler(
				AcceptsInterceptor.class, stepInvoker, new InstanceContainer(
						acceptsInterceptor));
		aspectHandler.execute(stack, controllerMethod, currentController);
		verify(acceptsInterceptor).accepts(controllerMethod);
		verify(acceptsInterceptor, never()).before();
		verify(acceptsInterceptor, never()).around(Mockito.same(stack),
				Mockito.same(controllerMethod),
				Mockito.any(ControllerInstance.class));
		verify(acceptsInterceptor, never()).after();
	}

	@Test(expected = IllegalStateException.class)
	public void shouldVerifyIfAcceptsMethodReturnsVoid() {
		VoidAcceptsInterceptor weirdInterceptor = new VoidAcceptsInterceptor();
		new AspectStyleInterceptorHandler(VoidAcceptsInterceptor.class,
				stepInvoker, new InstanceContainer(weirdInterceptor)).execute(
				stack, controllerMethod, currentController);
	}

	@Test(expected = IllegalStateException.class)
	public void shouldVerifyIfAcceptsMethodReturnsNonBooleanType() {
		NonBooleanAcceptsInterceptor weirdInterceptor = new NonBooleanAcceptsInterceptor();
		new AspectStyleInterceptorHandler(NonBooleanAcceptsInterceptor.class,
				stepInvoker, new InstanceContainer(weirdInterceptor)).execute(
				stack, controllerMethod, currentController);
	}

	@Test
	public void shouldInvokeAcceptsWithoutArgs() {
		AcceptsWithoutArgsInterceptor acceptsWithoutArgsInterceptor = spy(new AcceptsWithoutArgsInterceptor());
		AspectStyleInterceptorHandler aspectHandler = new AspectStyleInterceptorHandler(
				AcceptsWithoutArgsInterceptor.class, stepInvoker,
				new InstanceContainer(acceptsWithoutArgsInterceptor));
		aspectHandler.execute(stack, controllerMethod, currentController);
		InOrder order = inOrder(acceptsWithoutArgsInterceptor);
		order.verify(acceptsWithoutArgsInterceptor).accepts();
		order.verify(acceptsWithoutArgsInterceptor).before();
		order.verify(acceptsWithoutArgsInterceptor).around(
				Mockito.any(InterceptorStack.class),
				Mockito.same(controllerMethod),
				Mockito.any(ControllerInstance.class));
		order.verify(acceptsWithoutArgsInterceptor).after();
	}

	@Test
	public void shouldInvokeAroundWithSimpleStack() {
		ExampleOfSimpleStackInterceptor simpleStackInterceptor = spy(new ExampleOfSimpleStackInterceptor());
		AspectStyleInterceptorHandler aspectHandler = new AspectStyleInterceptorHandler(
				ExampleOfSimpleStackInterceptor.class, stepInvoker,
				new InstanceContainer(simpleStackInterceptor));
		aspectHandler.execute(stack, controllerMethod, currentController);
		verify(simpleStackInterceptor).around(
				Mockito.any(DefaultSimpleInterceptorStack.class));
	}

	@Test
	public void shouldInvokeNextIfNotAccepts() throws Exception {
		AcceptsInterceptor interceptor = spy(new AcceptsInterceptor(false));
		AspectStyleInterceptorHandler aspectHandler = new AspectStyleInterceptorHandler(
				AcceptsInterceptor.class, stepInvoker, new InstanceContainer(
						interceptor));
		aspectHandler.execute(stack, controllerMethod, null);
		verify(interceptor, never()).around(
				Mockito.any(InterceptorStack.class),
				Mockito.same(controllerMethod),
				Mockito.any(ControllerInstance.class));
		verify(stack).next(Mockito.same(controllerMethod),
				Mockito.any(ControllerInstance.class));
	}

	@Test
	public void shouldInvokeNotIfDoesNotHaveAround() throws Exception {
		WithoutAroundInterceptor interceptor = spy(new WithoutAroundInterceptor());
		AspectStyleInterceptorHandler aspectHandler = new AspectStyleInterceptorHandler(
				WithoutAroundInterceptor.class, stepInvoker,
				new InstanceContainer(interceptor));
		aspectHandler.execute(stack, controllerMethod, null);
		verify(stack).next(Mockito.same(controllerMethod),
				Mockito.any(ControllerInstance.class));
	}

	@Test(expected = IllegalArgumentException.class)
	public void mustReceiveStackAsParameterForAroundCall() {
		AroundInterceptorWithoutSimpleStackParameter interceptor = new AroundInterceptorWithoutSimpleStackParameter();
		AspectStyleInterceptorHandler aspectHandler = new AspectStyleInterceptorHandler(
				AroundInterceptorWithoutSimpleStackParameter.class,
				stepInvoker, new InstanceContainer(interceptor));
		aspectHandler.execute(stack, controllerMethod, currentController);
	}

	@Test(expected = IllegalArgumentException.class)
	public void mustNotReceiveStackAsParameterForBeforeAfterCall() {
		BeforeAfterInterceptorWithStackAsParameter interceptor = new BeforeAfterInterceptorWithStackAsParameter();
		AspectStyleInterceptorHandler aspectHandler = new AspectStyleInterceptorHandler(
				AroundInterceptorWithoutSimpleStackParameter.class,
				stepInvoker, new InstanceContainer(interceptor));
		aspectHandler.execute(stack, controllerMethod, currentController);
	}

	@Test(expected = IllegalArgumentException.class)
	public void mustNotReceiveStackAsParameterForAcceptsCall() {
		AcceptsInterceptorWithStackAsParameter interceptor = new AcceptsInterceptorWithStackAsParameter();
		AspectStyleInterceptorHandler aspectHandler = new AspectStyleInterceptorHandler(
				AroundInterceptorWithoutSimpleStackParameter.class,
				stepInvoker, new InstanceContainer(interceptor));
		aspectHandler.execute(stack, controllerMethod, currentController);
	}

	@Test
	public void shouldAcceptCustomizedAccepts() throws Exception {
		InterceptorWithCustomizedAccepts interceptor = spy(new InterceptorWithCustomizedAccepts());
		WithAnnotationAcceptor validator = mock(WithAnnotationAcceptor.class);		
		AspectStyleInterceptorHandler aspectHandler = new AspectStyleInterceptorHandler(
				AroundInterceptorWithoutSimpleStackParameter.class,
				stepInvoker, new InstanceContainer(interceptor,validator));
		DefaultControllerMethod home = new DefaultControllerMethod(
				new DefaultBeanClass(NotLoggedExampleController.class),
				NotLoggedExampleController.class.getDeclaredMethod("home"));
		when(validator.validate(Mockito.same(controllerMethod),Mockito.any(ControllerInstance.class))).thenReturn(true);
		aspectHandler.execute(stack, home, new NotLoggedExampleController());
		assertTrue(interceptor.isBeforeCalled());
		assertTrue(interceptor.isInterceptCalled());
		assertTrue(interceptor.isAfterCalled());
		
	}
	
	@Test
	public void shouldNotAcceptCustomizedAccepts() throws Exception {
		InterceptorWithCustomizedAccepts interceptor = new InterceptorWithCustomizedAccepts();
		WithAnnotationAcceptor validator = mock(WithAnnotationAcceptor.class);
		AspectStyleInterceptorHandler aspectHandler = new AspectStyleInterceptorHandler(
				AroundInterceptorWithoutSimpleStackParameter.class,
				stepInvoker, new InstanceContainer(interceptor,validator));
		DefaultControllerMethod home = new DefaultControllerMethod(
				new DefaultBeanClass(NotLoggedExampleController.class),
				NotLoggedExampleController.class.getDeclaredMethod("notAllowed"));
		when(validator.validate(Mockito.same(controllerMethod),Mockito.any(ControllerInstance.class))).thenReturn(false);
		aspectHandler.execute(stack, home, new NotLoggedExampleController());
		assertFalse(interceptor.isBeforeCalled());
		assertFalse(interceptor.isInterceptCalled());
		assertFalse(interceptor.isAfterCalled());
		
	}	

}