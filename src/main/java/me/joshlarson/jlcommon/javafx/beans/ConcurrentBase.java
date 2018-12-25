/***********************************************************************************
 * MIT License                                                                     *
 *                                                                                 *
 * Copyright (c) 2018 Josh Larson                                                  *
 *                                                                                 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy    *
 * of this software and associated documentation files (the "Software"), to deal   *
 * in the Software without restriction, including without limitation the rights    *
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell       *
 * copies of the Software, and to permit persons to whom the Software is           *
 * furnished to do so, subject to the following conditions:                        *
 *                                                                                 *
 * The above copyright notice and this permission notice shall be included in all  *
 * copies or substantial portions of the Software.                                 *
 *                                                                                 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR      *
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,        *
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE     *
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER          *
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,   *
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE   *
 * SOFTWARE.                                                                       *
 ***********************************************************************************/
package me.joshlarson.jlcommon.javafx.beans;

import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public class ConcurrentBase<T> implements Property<T> {
	
	private final Map<Object, ComplexListener<T, ConcurrentBase<T>>> listeners;
	
	private final Object mutex;
	private final boolean prohibitNull;
	
	private ObservableValue<? extends T> observable;
	private ChangeListener<T> bindUnidirectional;
	private T value;
	
	public ConcurrentBase() {
		this(false, null);
	}
	
	public ConcurrentBase(T value) {
		this(false, value);
	}
	
	protected ConcurrentBase(boolean prohibitNull, T value) {
		this.mutex = new Object();
		this.prohibitNull = prohibitNull;
		this.listeners = new HashMap<>();
		
		if (prohibitNull)
			Objects.requireNonNull(value, "Value cannot be set to null!");
		this.observable = null;
		this.bindUnidirectional = null;
		this.value = value;
	}
	
	@Override
	public void bind(ObservableValue<? extends T> observable) {
		this.observable = observable;
		this.bindUnidirectional = (obs, prev, next) -> internalSet(next);
		observable.addListener(bindUnidirectional);
	}
	
	@Override
	public void unbind() {
		ObservableValue<? extends T> observable = this.observable;
		observable.removeListener(bindUnidirectional);
		this.observable = null;
		this.bindUnidirectional = null;
	}
	
	@Override
	public boolean isBound() {
		return value != null || !prohibitNull;
	}
	
	@Override
	public void bindBidirectional(Property<T> other) {
		Bindings.bindBidirectional(this, other);
	}
	
	@Override
	public void unbindBidirectional(Property<T> other) {
		Bindings.unbindBidirectional(this, other);
	}
	
	@Override
	public Object getBean() {
		return value;
	}
	
	@Override
	public String getName() {
		return "";
	}
	
	@Override
	public void addListener(ChangeListener<? super T> listener) {
		addSimpleListener(listener, listener::changed);
	}
	
	@Override
	public void removeListener(ChangeListener<? super T> listener) {
		removeListener((Object) listener);
	}
	
	@Override
	public T getValue() {
		return internalGet();
	}
	
	@Override
	public void addListener(@NotNull InvalidationListener listener) {
		addSimpleListener(listener, (T val) -> listener.invalidated(this));
	}
	
	@Override
	public void removeListener(@NotNull InvalidationListener listener) {
		removeListener((Object) listener);
	}
	
	@Override
	public void setValue(T value) {
		internalSet(value);
	}
	
	public void addSimpleListener(@NotNull Consumer<T> listener) {
		addSimpleListener(listener, listener);
	}
	
	public <S> void addTransformListener(@NotNull Function<T, S> transformer, @NotNull Consumer<S> listener) {
		addTransformListener(listener, transformer, listener);
	}
	
	public void addSimpleListener(@NotNull Object key, @NotNull Consumer<T> listener) {
		addSimpleListener(key, (obs, prev, next) -> listener.accept(next));
	}
	
	public <S> void addTransformListener(@NotNull Object key, @NotNull Function<T, S> transformer, @NotNull Consumer<S> listener) {
		addSimpleListener(key, (obs, prev, next) -> listener.accept(transformer.apply(next)));
	}
	
	public void addSimpleListener(@NotNull ComplexListener<T, ConcurrentBase<T>> listener) {
		addSimpleListener(listener, listener);
	}
	
	public <S> void addTransformListener(@NotNull Function<T, S> transformer, @NotNull ComplexListener<S, ConcurrentBase<T>> listener) {
		addTransformListener(listener, transformer, listener);
	}
	
	public void addSimpleListener(@NotNull Object key, @NotNull ComplexListener<T, ConcurrentBase<T>> listener) {
		synchronized (listeners) {
			this.listeners.put(key, listener);
		}
	}
	
	public <S> void addTransformListener(@NotNull Object key, @NotNull Function<T, S> transformer, @NotNull ComplexListener<S, ConcurrentBase<T>> listener) {
		synchronized (listeners) {
			this.listeners.put(key, (obs, prev, next) -> listener.accept(obs, transformer.apply(prev), transformer.apply(next)));
		}
	}
	
	public boolean removeListener(@NotNull Object key) {
		synchronized (listeners) {
			return listeners.remove(key) != null;
		}
	}
	
	public void clearListeners() {
		synchronized (listeners) {
			listeners.clear();
		}
	}
	
	public void callListeners() {
		T value = this.value;
		callListeners(value, value);
	}
	
	@Override
	@NotNull
	public String toString() {
		T value = this.value;
		return value == null ? "null" : value.toString();
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof ConcurrentBase))
			return false;
		Object myValue = this.value;
		Object theirValue = ((ConcurrentBase) o).value;
		return myValue == theirValue || (myValue != null && myValue.equals(theirValue));
	}
	
	protected T internalGet() {
		return value;
	}
	
	protected T internalSet(T value) {
		if (prohibitNull)
			Objects.requireNonNull(value, "Value cannot be set to null!");
		synchronized (getMutex()) {
			T prev = this.value;
			this.value = value;
			if (prev != value && (prev == null || !prev.equals(value)))
				callListeners(prev, value);
			return prev;
		}
	}
	
	protected void callListeners(T prev, T next) {
		synchronized (listeners) {
			for (ComplexListener<T, ConcurrentBase<T>> listener : listeners.values()) {
				listener.accept(this, prev, next);
			}
		}
	}
	
	protected Object getMutex() {
		return mutex;
	}
	
	public interface ComplexListener<S, U extends ConcurrentBase<?>> {
		void accept(@NotNull U concurrentObject, S prev, S next);
	}
	
}
