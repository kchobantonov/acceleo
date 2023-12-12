/*******************************************************************************
 * Copyright (c) 2023 Obeo.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors:
 *     Obeo - initial API and implementation
 *******************************************************************************/
package org.eclipse.acceleo.query.ide.jdt.validation.type;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.eclipse.acceleo.query.runtime.IReadOnlyQueryEnvironment;
import org.eclipse.acceleo.query.validation.type.AbstractType;
import org.eclipse.acceleo.query.validation.type.EClassifierType;
import org.eclipse.acceleo.query.validation.type.IJavaType;
import org.eclipse.acceleo.query.validation.type.ISymbolicType;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;

/**
 * An implementation of {@link org.eclipse.acceleo.query.validation.type.IType IType} for Eclipse JDT
 * {@link IType}.
 * 
 * @author <a href="mailto:yvan.lussaud@obeo.fr">Yvan Lussaud</a>
 */
public class EclipseJDTITypeType extends AbstractType implements ISymbolicType {

	/**
	 * The mapping from a {@link Class} qualified name to its wrapper {@link Class} qualified name.
	 */
	public static final Map<String, String> PRIMITIVE_WRAPPER_NAMES;

	/**
	 * The {@link Objects} class qualified name.
	 */
	private static final Object OBJECT_CLASS_NAME = Object.class.getName();

	static {
		final Map<String, String> map = new HashMap<String, String>();

		for (Entry<Class<?>, Class<?>> entry : PRIMITIVE_WRAPPERS.entrySet()) {
			map.put(entry.getKey().getCanonicalName(), entry.getValue().getCanonicalName());
		}

		PRIMITIVE_WRAPPER_NAMES = Collections.unmodifiableMap(map);
	}

	/**
	 * The {@link IReadOnlyQueryEnvironment}.
	 */
	private final IReadOnlyQueryEnvironment queryEnvironment;

	/**
	 * The Eclipse JDT {@link IType}.
	 */
	private final IType jdtType;

	/**
	 * Constructor.
	 * 
	 * @param queryEnvironment
	 *            the {@link IReadOnlyQueryEnvironment}
	 * @param jdtType
	 *            the Eclipse JDT {@link IType}
	 */
	public EclipseJDTITypeType(IReadOnlyQueryEnvironment queryEnvironment, IType jdtType) {
		this.jdtType = jdtType;
		this.queryEnvironment = Objects.requireNonNull(queryEnvironment);
	}

	@Override
	public IType getType() {
		return jdtType;
	}

	@Override
	public boolean isAssignableFrom(org.eclipse.acceleo.query.validation.type.IType type) {
		boolean res;

		if (getType() == null) {
			res = false;
		} else if (type instanceof EclipseJDTITypeType) {
			final IType otherJdtType = ((EclipseJDTITypeType)type).getType();
			res = isAssignableFrom(getType(), otherJdtType);
		} else if (type instanceof IJavaType) {
			final Class<?> otherClass = ((IJavaType)type).getType();
			try {
				final IType otherJdtType = getType().getJavaProject().findType(otherClass.getCanonicalName());
				res = isAssignableFrom(getType(), otherJdtType);
			} catch (JavaModelException e) {
				res = false;
			}
			// TODO collection type
		} else if (type instanceof EClassifierType) {
			final Class<?> otherClass = queryEnvironment.getEPackageProvider().getClass(
					((EClassifierType)type).getType());
			if (otherClass != null) {
				try {
					final IType otherJdtType = getType().getJavaProject().findType(otherClass
							.getCanonicalName());
					res = isAssignableFrom(getType(), otherJdtType);
				} catch (JavaModelException e) {
					res = false;
				}
			} else {
				// instances will be dynamic DynamicEObjectImpl
				// this suppose All dynamic instances of EClass are EObjects
				try {
					final IType eObjectJdtType = getType().getJavaProject().findType(EObject.class
							.getCanonicalName());
					res = getType() == eObjectJdtType && ((EClassifierType)type).getType() instanceof EClass;
				} catch (JavaModelException e) {
					res = false;
				}
			}
		} else {
			res = false;
		}

		return res;
	}

	private boolean isAssignableFrom(final IType jdtType, final IType otherJdtType) {
		boolean res;

		// wrapped types are not a problem here because IType are already wrapped
		if (jdtType == null) {
			res = false;
		} else if (otherJdtType == null) {
			res = true;
		} else if (jdtType.equals(otherJdtType)) {
			res = true;
		} else {
			try {
				ITypeHierarchy superTypeHierarchy = otherJdtType.newSupertypeHierarchy(
						new NullProgressMonitor());
				res = superTypeHierarchy.contains(jdtType);
			} catch (JavaModelException e) {
				res = false;
			}
		}

		return res;
	}

	@Override
	public boolean isCompatibleSymbol(String symbol) {
		boolean res;

		final IType type = getType();
		if (type == null) {
			res = false;
		} else if (type.getFullyQualifiedName().equals(symbol) || OBJECT_CLASS_NAME.equals(symbol)) {
			res = true;
		} else {
			try {
				IType otherJdtType = type.getJavaProject().findType(symbol);
				if (otherJdtType != null) {
					res = isAssignableFrom(type, otherJdtType);
				} else {
					res = false;
				}
			} catch (JavaModelException e) {
				res = false;
			}
		}

		return res;
	}

	@Override
	public int hashCode() {
		final int result;

		final IType type = getType();
		if (type != null) {
			result = type.hashCode();
		} else {
			result = 0;
		}

		return result;
	}

	@Override
	public boolean equals(Object obj) {
		final boolean result;

		final IType type = getType();
		if (type != null) {
			result = obj instanceof EclipseJDTITypeType && type.equals(((EclipseJDTITypeType)obj).getType());
		} else {
			result = obj instanceof EclipseJDTITypeType && ((EclipseJDTITypeType)obj).getType() == null;
		}

		return result;
	}

	@Override
	public String toString() {
		final String res;

		final IType type = getType();
		if (type != null) {
			res = "JDT=" + type.getFullyQualifiedName();
		} else {
			res = "JDT=null";
		}

		return res;
	}

}
