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
package org.eclipse.acceleo.query.ide.jdt.runtime.impl;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.acceleo.query.ide.jdt.Activator;
import org.eclipse.acceleo.query.ide.jdt.validation.type.EclipseJDTITypeType;
import org.eclipse.acceleo.query.runtime.IReadOnlyQueryEnvironment;
import org.eclipse.acceleo.query.runtime.impl.AbstractService;
import org.eclipse.acceleo.query.runtime.impl.JavaMethodService;
import org.eclipse.acceleo.query.runtime.namespace.ILoader;
import org.eclipse.acceleo.query.validation.type.IType;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

/**
 * Implementation of an {@link org.eclipse.acceleo.query.runtime.IService IService} for Eclipse JDT
 * {@link IMethod}.
 * 
 * @author <a href="mailto:yvan.lussaud@obeo.fr">Yvan Lussaud</a>
 */
public class EclipseJDTIMethodService extends AbstractService<IMethod> {

	/**
	 * Constructor.
	 * 
	 * @param method
	 *            the {@link IMethod}
	 */
	public EclipseJDTIMethodService(IMethod method) {
		super(method);
	}

	@Override
	public String getName() {
		return getOrigin().getElementName();
	}

	@Override
	public String getShortSignature() {
		try {
			return getOrigin().getSignature();
		} catch (JavaModelException e) {
			return getName() + "(...)";
		}
	}

	@Override
	public String getLongSignature() {
		return getOrigin().getDeclaringType() + "::" + getShortSignature();
	}

	@Override
	public List<IType> computeParameterTypes(IReadOnlyQueryEnvironment queryEnvironment) {
		final List<IType> res = new ArrayList<IType>();

		try {
			for (ILocalVariable parameter : getOrigin().getParameters()) {
				final org.eclipse.jdt.core.IType type = getITypeFromSignature(parameter.getTypeSignature());
				res.add(getITypeType(queryEnvironment, type));
			}
		} catch (JavaModelException e) {
			Activator.getPlugin().getLog().log(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
					"can't get parameter type from: " + getLongSignature(), e));
		}

		return res;
	}

	private org.eclipse.jdt.core.IType getITypeFromSignature(final String typeSignature)
			throws JavaModelException {
		final org.eclipse.jdt.core.IType res;

		final String signatureSimpleName = Signature.getSignatureSimpleName(typeSignature);

		if (!"void".equals(signatureSimpleName)) {
			final String wrappedName = EclipseJDTITypeType.PRIMITIVE_WRAPPER_NAMES.getOrDefault(
					signatureSimpleName, signatureSimpleName);
			final String[][] resolvedType = getOrigin().getDeclaringType().resolveType(wrappedName);
			if (resolvedType != null) {
				res = getOrigin().getJavaProject().findType(resolvedType[0][0] + ILoader.DOT
						+ resolvedType[0][1]);
			} else {
				res = null;
			}
		} else {
			res = null;
		}

		return res;
	}

	private IType getITypeType(IReadOnlyQueryEnvironment queryEnvironment, org.eclipse.jdt.core.IType type) {
		// TODO Collection types
		return new EclipseJDTITypeType(queryEnvironment, type);
	}

	@Override
	public int getNumberOfParameters() {
		return getOrigin().getNumberOfParameters();
	}

	@Override
	public int getPriority() {
		return JavaMethodService.PRIORITY;
	}

	@Override
	public Set<IType> computeType(IReadOnlyQueryEnvironment queryEnvironment) {
		final Set<IType> res = new LinkedHashSet<IType>();

		try {
			final org.eclipse.jdt.core.IType returnType = getITypeFromSignature(getOrigin().getReturnType());
			res.addAll(getIType(queryEnvironment, returnType));
		} catch (JavaModelException e) {
			Activator.getPlugin().getLog().log(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
					"can't get return type from: " + getLongSignature(), e));
		}

		return res;
	}

	private Set<IType> getIType(IReadOnlyQueryEnvironment queryEnvironment,
			org.eclipse.jdt.core.IType returnType) {
		final Set<IType> res = new LinkedHashSet<>();

		res.add(new EclipseJDTITypeType(queryEnvironment, returnType));

		return res;
	}

	@Override
	protected Object internalInvoke(Object[] arguments) throws Exception {
		throw new UnsupportedOperationException("This service can't be used at runtime.");
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof EclipseJDTIMethodService && ((EclipseJDTIMethodService)obj).getOrigin().equals(
				getOrigin());
	}

	@Override
	public int hashCode() {
		return getOrigin().hashCode();
	}

}
