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
package org.eclipse.acceleo.query.validation.type;

/**
 * Symbolic type resolution. This can be useful when class can be invalid or should not be loaded. A symbol
 * can be anything, but whenever possible use a {@link Class}'s {@link Class#getCanonicalName() canonical
 * name}.
 * 
 * @author <a href="mailto:yvan.lussaud@obeo.fr">Yvan Lussaud</a>
 */
public interface ISymbolicType extends IType {

	public boolean isCompatibleSymbol(String symbol);

}
