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
package org.eclipse.acceleo.aql.validation.quickfixes;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.acceleo.Module;
import org.eclipse.acceleo.aql.validation.IAcceleoValidationResult;
import org.eclipse.acceleo.query.ast.ASTNode;
import org.eclipse.acceleo.query.parser.quickfixes.IAstQuickFix;
import org.eclipse.acceleo.query.runtime.IValidationMessage;
import org.eclipse.acceleo.query.runtime.ValidationMessageLevel;
import org.eclipse.acceleo.query.runtime.namespace.IQualifiedNameQueryEnvironment;
import org.eclipse.emf.ecore.util.ComposedSwitch;

public class AcceleoQuickFixesSwitch extends ComposedSwitch<List<IAstQuickFix>> {

	/**
	 * The {@link IAcceleoValidationResult}.
	 */
	private IAcceleoValidationResult validationResult;

	/**
	 * Constructor.
	 * 
	 * @param queryEnvironment
	 *            the {@link IQualifiedNameQueryEnvironment}
	 * @param validationResult
	 *            the {@link IAcceleoValidationResult}
	 * @param moduleText
	 *            the text representation of the {@link Module}
	 */
	public AcceleoQuickFixesSwitch(IQualifiedNameQueryEnvironment queryEnvironment,
			IAcceleoValidationResult validationResult, String moduleQualifiedName, String moduleText) {
		super();
		addSwitch(new AqlQuickFixesSwitch(queryEnvironment, validationResult, moduleQualifiedName,
				moduleText));
		addSwitch(new ModuleQuickFixesSwitch(queryEnvironment, validationResult, moduleQualifiedName,
				moduleText));
		this.validationResult = validationResult;
	}

	/**
	 * Gets the {@link List} of {@link IAstQuickFix} for the given {@link ASTNode}.
	 * 
	 * @param node
	 *            the node
	 * @return the {@link List} of {@link IAstQuickFix} for the given {@link ASTNode}
	 */
	public List<IAstQuickFix> getQuickFixes(ASTNode node) {
		final List<IAstQuickFix> res;

		final List<IValidationMessage> errors = validationResult.getValidationMessages(node).stream().filter(
				m -> m.getLevel() == ValidationMessageLevel.ERROR).collect(Collectors.toList());
		if (!errors.isEmpty()) {
			final List<IAstQuickFix> quickFixes = doSwitch(node);
			if (quickFixes != null) {
				res = quickFixes;
			} else {
				res = Collections.emptyList();
			}
		} else {
			res = Collections.emptyList();
		}

		return res;
	}

}
