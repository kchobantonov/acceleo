/*******************************************************************************
 * Copyright (c) 2020, 2023 Obeo.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors:
 *     Obeo - initial API and implementation
 *******************************************************************************/
package org.eclipse.acceleo.query.ide.jdt.runtime.impl.namespace;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.acceleo.query.ide.jdt.Activator;
import org.eclipse.acceleo.query.ide.runtime.impl.namespace.EclipseQualifiedNameResolver;
import org.eclipse.acceleo.query.runtime.impl.namespace.ClassLoaderQualifiedNameResolver;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.osgi.container.Module;

/**
 * Eclipse JDT {@link ClassLoaderQualifiedNameResolver}.
 * 
 * @author <a href="mailto:yvan.lussaud@obeo.fr">Yvan Lussaud</a>
 */
public class EclipseJDTQualifiedNameResolver extends ClassLoaderQualifiedNameResolver {

	/**
	 * The {@link IJavaProject}.
	 */
	private final IJavaProject project;

	/**
	 * For workspace use, local project resolution only.
	 */
	private boolean forWorkspace;

	/**
	 * Constructor.
	 * 
	 * @param classLoader
	 *            the {@link ClassLoader}
	 * @param project
	 *            the {@link IProject}
	 * @param qualifierSeparator
	 *            the qualifier name separator
	 * @param forWorkspace
	 *            <code>true</code> for workspace use, local project resolution only
	 */
	public EclipseJDTQualifiedNameResolver(ClassLoader classLoader, IProject project,
			String qualifierSeparator, boolean forWorkspace) {
		super(createProjectClassLoader(classLoader, project, forWorkspace), qualifierSeparator);
		this.project = JavaCore.create(project);
		this.forWorkspace = forWorkspace;
	}

	/**
	 * Creates the class loader for the given {@link IProject}.
	 * 
	 * @param classLoader
	 *            the parent {@link ClassLoader}
	 * @param project
	 *            the {@link IProject}
	 * @param forWorspace
	 *            <code>true</code> for workspace use, local project resolution only
	 * @return the class loader for the given {@link IProject}
	 */
	protected static ClassLoader createProjectClassLoader(ClassLoader classLoader, IProject project,
			boolean forWorspace) {
		ClassLoader res;

		if (project.exists() && project.isOpen()) {
			final IJavaProject javaProject = JavaCore.create(project);
			if (javaProject != null) {
				try {
					final String[] classPathEntries = getClassPathes(javaProject, forWorspace);
					final List<URL> urlList = new ArrayList<URL>();
					for (String entry : classPathEntries) {
						final IPath path = new Path(entry);
						final URL url = path.toFile().toURI().toURL();
						urlList.add(url);
					}
					final URL[] urls = (URL[])urlList.toArray(new URL[urlList.size()]);
					res = new URLClassLoader(urls, classLoader);
				} catch (CoreException e) {
					Activator.getPlugin().getLog().log(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
							EclipseQualifiedNameResolver.CAN_T_LOAD_FROM_WORKSPACE, e));
					res = EclipseQualifiedNameResolver.createProjectClassLoader(classLoader, project);
				} catch (MalformedURLException e) {
					Activator.getPlugin().getLog().log(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
							EclipseQualifiedNameResolver.CAN_T_LOAD_FROM_WORKSPACE, e));
					res = EclipseQualifiedNameResolver.createProjectClassLoader(classLoader, project);
				}
			} else {
				res = EclipseQualifiedNameResolver.createProjectClassLoader(classLoader, project);
			}
		} else {
			res = classLoader;
		}

		return res;
	}

	// copied from JavaRuntime.computeDefaultRuntimeClassPath()
	private static String[] getClassPathes(IJavaProject javaProject, boolean forWorkspace)
			throws CoreException {
		IRuntimeClasspathEntry[] unresolved = JavaRuntime.computeUnresolvedRuntimeClasspath(javaProject);

		// 1. remove bootpath entries
		// 2. resolve & translate to local file system paths
		List<String> resolved = new ArrayList<>(unresolved.length);
		for (int i = 0; i < unresolved.length; i++) {
			IRuntimeClasspathEntry entry = unresolved[i];
			if (entry.getClasspathProperty() == IRuntimeClasspathEntry.USER_CLASSES) {
				IRuntimeClasspathEntry[] entries = JavaRuntime.resolveRuntimeClasspathEntry(entry,
						javaProject);
				for (int j = 0; j < entries.length; j++) {
					if (keepEntry(javaProject, forWorkspace, entries[j])) {
						String location = entries[j].getLocation();
						if (location != null) {
							resolved.add(location);
						}
					}
				}
			}
		}

		return resolved.toArray(new String[resolved.size()]);

	}

	/**
	 * Tells if we should keep the given {@link IRuntimeClasspathEntry}.
	 * 
	 * @param javaProject
	 *            the current {@link IJavaProject}
	 * @param forWorspace
	 *            <code>true</code> for workspace use, local project resolution only
	 * @param entry
	 *            the {@link IRuntimeClasspathEntry}
	 * @return <code>true</code> if we should keep the given {@link IRuntimeClasspathEntry},
	 *         <code>false</code> otherwise
	 */
	private static boolean keepEntry(IJavaProject javaProject, boolean forWorkspace,
			IRuntimeClasspathEntry entry) {
		return !forWorkspace || entry.getType() != IRuntimeClasspathEntry.PROJECT || javaProject
				.getProject() == entry.getResource();
	}

	@Override
	public URI getSourceURI(String qualifiedName) {
		URI res;

		if (project != null) {
			try {
				final URI foundURI = getSourceURI(project, qualifiedName);
				if (foundURI != null) {
					res = foundURI;
				} else {
					res = super.getSourceURI(qualifiedName);
				}
			} catch (JavaModelException e) {
				res = super.getSourceURI(qualifiedName);
			}
		} else {
			res = super.getSourceURI(qualifiedName);
		}

		return res;
	}

	/**
	 * Gets the source {@link URI} corresponding to the given {@link Module} qualified name for the given
	 * {@link IJavaProject}.
	 * 
	 * @param javaProject
	 *            the {@link IJavaProject}
	 * @param qualifiedName
	 *            the qualified name (e.g. <code>qualified::path::to::module</code>)
	 * @throws JavaModelException
	 *             if the class path of the given {@link IJavaProject} can't be resolved.
	 * @return the source {@link URI} corresponding to the given {@link Module} qualified name for the given
	 *         {@link IJavaProject} if any, <code>null</code> otherwise
	 */
	private URI getSourceURI(IJavaProject javaProject, String qualifiedName) throws JavaModelException {
		URI res = null;

		found: for (IClasspathEntry entry : javaProject.getResolvedClasspath(true)) {
			if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
				final IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
				final String srcFolder = entry.getPath().toString();
				final List<String> relativePathes = getPossibleResourceNames(qualifiedName);
				for (String relativePath : relativePathes) {
					final URI uri = workspaceRoot.getFile(new Path(srcFolder + "/" + relativePath))
							.getLocationURI();
					final File file = new File(uri);
					if (file.isFile() && file.exists()) {
						res = file.toURI();
						break found;
					}
				}
			} else if (!forWorkspace && entry.getEntryKind() == IClasspathEntry.CPE_PROJECT) {
				final IProject childProject = ResourcesPlugin.getWorkspace().getRoot().getProject(entry
						.getPath().lastSegment());
				if (childProject != null) {
					final IJavaProject childJavaProject = JavaCore.create(childProject);
					res = getSourceURI(childJavaProject, qualifiedName);
				}
			}
		}

		return res;
	}

	@Override
	public URI getBinaryURI(URI sourceURI) {
		URI res;

		if (project != null) {
			try {
				final URI foundURI = getBinaryURI(project, sourceURI);
				if (foundURI != null) {
					res = foundURI;
				} else {
					res = super.getBinaryURI(sourceURI);
				}
			} catch (JavaModelException e) {
				res = super.getBinaryURI(sourceURI);
			}
		} else {
			res = super.getBinaryURI(sourceURI);
		}

		return res;
	}

	private URI getBinaryURI(IJavaProject javaProject, URI sourceURI) throws JavaModelException {
		final URI res;

		final IWorkspaceRoot workspaceRoot = javaProject.getProject().getWorkspace().getRoot();
		final IFile sourceFile = workspaceRoot.getFileForLocation(new Path(sourceURI.getPath()));
		final IClasspathEntry entry = getContainingEntry(javaProject, sourceFile);
		if (entry != null) {
			if (entry.getContentKind() == IPackageFragmentRoot.K_BINARY) {
				res = sourceURI;
			} else if (entry.getContentKind() == IPackageFragmentRoot.K_SOURCE) {
				// TODO check forWorkspace and the project to the class path entry
				final IPath relativePath = sourceFile.getFullPath().makeRelativeTo(entry.getPath());
				final IPath binaryPath = javaProject.getOutputLocation().append(relativePath);
				final IFile binaryFile = workspaceRoot.getFile(binaryPath);
				if (binaryFile.exists()) {
					res = binaryFile.getLocationURI();
				} else {
					res = null;
				}
			} else {
				throw new IllegalStateException("unknown classpath entry content kind.");
			}
		} else {
			res = null;
		}

		return res;
	}

	private IClasspathEntry getContainingEntry(IJavaProject javaProject, final IFile sourceFile)
			throws JavaModelException {
		final IClasspathEntry res;

		final IClasspathEntry foundEntry = javaProject.findContainingClasspathEntry(sourceFile);
		if (foundEntry != null) {
			res = foundEntry;
		} else {
			IClasspathEntry fallbackFound = null;
			for (IClasspathEntry entry : javaProject.getResolvedClasspath(true)) {
				if (entry.getPath().isPrefixOf(sourceFile.getFullPath())) {
					fallbackFound = entry;
					break;
				}
			}
			res = fallbackFound;
		}

		return res;
	}

	/**
	 * Gets the {@link IProject}.
	 * 
	 * @return the {@link IProject}
	 */
	protected IJavaProject getProject() {
		return project;
	}

}
