/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jan S. Rellermeyer, IBM Research - initial API and implementation
 *******************************************************************************/
package org.eclipse.concierge.compat.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.concierge.BundleImpl.Revision;
import org.eclipse.concierge.Concierge;
import org.eclipse.concierge.Resources.BundleCapabilityImpl;
import org.eclipse.concierge.Resources.BundleRequirementImpl;
import org.eclipse.concierge.Tuple;
import org.eclipse.concierge.Utils;
import org.eclipse.concierge.compat.LegacyBundleProcessing;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.AbstractWiringNamespace;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.ExecutionEnvironmentNamespace;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.resource.Namespace;

public class BundleManifestTwo implements LegacyBundleProcessing {

	@SuppressWarnings("deprecation")
	private static final String SPECIFICATION_VERSION = Constants.PACKAGE_SPECIFICATION_VERSION;

	@SuppressWarnings("deprecation")
	private static final String BUNDLE_REQUIREDEXECUTIONENVIRONMENT = Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT;

	private static final Pattern PARSE_EE = Pattern
			.compile("([^-|\\.]*)(?:\\-(\\d\\.\\d))(?:\\/([^\\d|\\.|\\/]*))|([^\\d|\\.|\\/]*)(?:\\/([^-|\\.]*)(?:\\-(\\d\\.\\d)))|([^\\.|\\d]*)");

	private static final Pattern PARSE_EE2 = Pattern
			.compile("([^-|\\.]*)(?:\\-(\\d\\.\\d))?(?:\\/([^-|\\.]*)(?:\\-(\\d\\.\\d))?)?");

	public Tuple<List<BundleCapability>, List<BundleRequirement>> processManifest(
			final Revision revision, final Manifest manifest)
			throws BundleException {
		final Attributes mfAttrs = manifest.getMainAttributes();

		final List<BundleCapability> caps = new ArrayList<BundleCapability>();
		final List<BundleRequirement> reqs = new ArrayList<BundleRequirement>();

		final String bundleSymbolicName;
		final Version bundleVersion;

		final HashMap<String, Object> symbolicNameAttrs;

		{
			final String symNameStr = mfAttrs
					.getValue(Constants.BUNDLE_SYMBOLICNAME);
			if (symNameStr == null) {
				throw new BundleException("Bundle with "
						+ Constants.BUNDLE_MANIFESTVERSION + "=2 must specify "
						+ Constants.BUNDLE_SYMBOLICNAME);
			}

			final String[] parts = symNameStr.split(Utils.SPLIT_AT_SEMICOLON);
			if (parts[0].contains(";")) {
				throw new IllegalArgumentException(symNameStr);
			}

			final Tuple<HashMap<String, String>, HashMap<String, Object>> tuple = Utils
					.parseLiterals(parts, 1);

			symbolicNameAttrs = tuple.getLatter();

			final String fragHostStr = mfAttrs
					.getValue(Constants.FRAGMENT_HOST);

			// identity namespace and bundle namespace capability
			{
				final Map<String, Object> attrs = new HashMap<String, Object>(symbolicNameAttrs);
				final Map<String, String> dirs = new HashMap<String, String>();

				bundleSymbolicName = parts[0].trim();

				attrs.put(BundleNamespace.BUNDLE_NAMESPACE, bundleSymbolicName);

				try {
					bundleVersion = Version.parseVersion(mfAttrs
							.getValue(Constants.BUNDLE_VERSION));
					attrs.put(
							BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE,
							bundleVersion);
				} catch (final IllegalArgumentException e) {
					throw new BundleException(
							"Syntactic error in bundle manifest", e);
				}

				final String singletonStr = (String) tuple.getFormer().get(
						Constants.SINGLETON_DIRECTIVE);
				dirs.put(IdentityNamespace.CAPABILITY_SINGLETON_DIRECTIVE,
						singletonStr == null ? "false" : singletonStr.trim());

				final String mandatory = tuple.getFormer().get(Constants.MANDATORY_DIRECTIVE);
				if (mandatory != null) {
					dirs.put(BundleNamespace.CAPABILITY_MANDATORY_DIRECTIVE, mandatory);
				}

				if (fragHostStr == null) {
					final BundleCapability bundle = new BundleCapabilityImpl(
							revision, BundleNamespace.BUNDLE_NAMESPACE, dirs,
							attrs, "Bundle " + bundleSymbolicName + ' '
									+ bundleVersion);

					caps.add(bundle);
				}

				final Map<String, Object> attrs2 = new HashMap<String, Object>(
						attrs);
				final Map<String, String> dirs2 = new HashMap<String, String>(
						dirs);
				attrs2.remove(BundleNamespace.BUNDLE_NAMESPACE);
				attrs2.put(IdentityNamespace.IDENTITY_NAMESPACE,
						bundleSymbolicName);
				attrs2.remove(BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE);
				attrs2.put(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE,
						bundleVersion);

				// TODO: handle unknown
				attrs2.put(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE,
						fragHostStr != null ? IdentityNamespace.TYPE_FRAGMENT
								: IdentityNamespace.TYPE_BUNDLE);

				addAttributeIfPresent(attrs2,
						IdentityNamespace.CAPABILITY_COPYRIGHT_ATTRIBUTE,
						mfAttrs, Constants.BUNDLE_COPYRIGHT);
				addAttributeIfPresent(attrs2,
						IdentityNamespace.CAPABILITY_DESCRIPTION_ATTRIBUTE,
						mfAttrs, Constants.BUNDLE_DESCRIPTION);
				addAttributeIfPresent(attrs2,
						IdentityNamespace.CAPABILITY_DOCUMENTATION_ATTRIBUTE,
						mfAttrs, Constants.BUNDLE_DOCURL);
				addAttributeIfPresent(attrs2,
						IdentityNamespace.CAPABILITY_LICENSE_ATTRIBUTE,
						mfAttrs, "Bundle-License");

				final BundleCapability identity = new BundleCapabilityImpl(
						revision, IdentityNamespace.IDENTITY_NAMESPACE, dirs2,
						attrs2, "Identity " + bundleSymbolicName + ' '
								+ bundleVersion);

				caps.add(identity);
			}

			// host namespace
			{
				if (fragHostStr != null) {
					final Map<String, String> dirs2 = new HashMap<String, String>();

					final String[] parts2 = fragHostStr
							.split(Utils.SPLIT_AT_SEMICOLON);
					if (parts2[0].contains(";")) {
						throw new IllegalArgumentException(fragHostStr);
					}

					final Tuple<HashMap<String, String>, HashMap<String, Object>> tuple2 = Utils
							.parseLiterals(parts2, 1);

					if (tuple2.getFormer() != null) {
						dirs2.putAll(tuple2.getFormer());
					}

					// must not have uses directive
					dirs2.remove(AbstractWiringNamespace.CAPABILITY_USES_DIRECTIVE);

					// must not have effective directive
					dirs2.remove(AbstractWiringNamespace.CAPABILITY_EFFECTIVE_DIRECTIVE);

					dirs2.put(Namespace.REQUIREMENT_FILTER_DIRECTIVE, Utils
							.createFilter(HostNamespace.HOST_NAMESPACE,
									parts2[0], tuple2.getLatter()));

					// fragments can attach to multiple hosts
					dirs2.put(Namespace.REQUIREMENT_CARDINALITY_DIRECTIVE,
							Namespace.CARDINALITY_MULTIPLE);

					// some attributes for convenience
					dirs2.put(HostNamespace.HOST_NAMESPACE, parts2[0]);
					final String versionRange = (String) tuple2.getLatter()
							.get(Constants.BUNDLE_VERSION_ATTRIBUTE);
					if (versionRange != null) {
						dirs2.put(
								HostNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE,
								versionRange);
					}

					final BundleRequirement hostReq = new BundleRequirementImpl(
							revision,
							HostNamespace.HOST_NAMESPACE,
							dirs2,
							null,
							Constants.FRAGMENT_HOST
									+ ' '
									+ parts2[0]
									+ ' '
									+ (versionRange == null ? "" : versionRange));
					reqs.add(hostReq);
				} else {
					if (!Constants.FRAGMENT_ATTACHMENT_NEVER.equals(tuple
							.getFormer().get(
									Constants.FRAGMENT_ATTACHMENT_DIRECTIVE))) {
						final Map<String, Object> attrs = new HashMap<String, Object>();
						final Map<String, String> dirs = new HashMap<String, String>();

						if (tuple.getFormer() != null) {
							dirs.putAll(tuple.getFormer());
						}

						attrs.putAll(symbolicNameAttrs);

						attrs.put(HostNamespace.HOST_NAMESPACE,
								bundleSymbolicName);
						attrs.put(
								HostNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE,
								bundleVersion);

						// must not have uses directive
						dirs.remove(AbstractWiringNamespace.CAPABILITY_USES_DIRECTIVE);

						// must not have effective directive
						dirs.remove(AbstractWiringNamespace.CAPABILITY_EFFECTIVE_DIRECTIVE);

						final BundleCapability hostCap = new BundleCapabilityImpl(
								revision, HostNamespace.HOST_NAMESPACE, dirs,
								attrs, "Host " + bundleSymbolicName + ' '
										+ bundleVersion);
						caps.add(hostCap);
					}
				}
			}
		}

		// execution environment
		final String requiredEEStr = mfAttrs
				.getValue(BUNDLE_REQUIREDEXECUTIONENVIRONMENT);

		if (requiredEEStr != null) {
			final String[] requiredEEs = requiredEEStr.split(",");
			final StringBuffer buffer = new StringBuffer();

			buffer.append("osgi.ee; filter:=\"");
			if (requiredEEs.length > 1) {
				buffer.append("(|");
			}

			for (int i = 0; i < requiredEEs.length; i++) {
				final String requiredEE = requiredEEs[i].trim();
				final Matcher matcher = PARSE_EE.matcher(requiredEE);

				final String ee1;
				final String ee2;
				final String version;

				if (matcher.matches()) {
					if (matcher.group(1) != null) {
						ee1 = matcher.group(1);
						ee2 = matcher.group(3);
						version = matcher.group(2);
					} else if (matcher.group(4) != null) {
						ee1 = matcher.group(4);
						ee2 = matcher.group(5);
						version = matcher.group(6);
					} else {
						final String s = matcher.group(7);
						final int pos = s.indexOf('/');
						ee1 = pos > -1 ? s.substring(0, pos) : s;
						ee2 = pos > -1 ? s.substring(pos + 1, s.length())
								: null;
						version = null;
					}
				} else {
					final Matcher matcher2 = PARSE_EE2.matcher(requiredEE);

					if (matcher2.matches()) {
						if (matcher2.group(4) == null
								|| matcher2.group(2).equals(matcher2.group(4))) {
							ee1 = matcher2.group(1);
							version = matcher2.group(2);
							ee2 = matcher2.group(3);
						} else {
							ee1 = matcher2.group(1) + "-" + matcher2.group(2);
							ee2 = matcher2.group(3) + "-" + matcher2.group(4);
							version = null;
						}
					} else {
						throw new BundleException("invalid bree string "
								+ requiredEE);
					}
				}

				if (version != null) {
					buffer.append("(&(");
				} else {
					buffer.append('(');
				}
				buffer.append(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE);
				buffer.append('=');
				buffer.append("J2SE".equals(ee1) ? "JavaSE" : ee1);
				if (ee2 != null) {
					buffer.append('/');
					buffer.append(ee2);
				}
				if (version != null) {
					buffer.append(")(version=");
					buffer.append(version);
					buffer.append("))");
				} else {
					buffer.append(')');
				}
			}

			if (requiredEEs.length > 1) {
				buffer.append(')');
			}
			buffer.append('\"');

			reqs.add(new BundleRequirementImpl(revision, buffer.toString()));
		}

		// package namespace
		{
			final Set<String> importSet = new HashSet<String>();

			// package imports
			final String importStr = mfAttrs.getValue(Constants.IMPORT_PACKAGE);
			if (importStr != null) {
				final String[] imports = importStr.split(Utils.SPLIT_AT_COMMA);
				for (int i = 0; i < imports.length; i++) {
					final String[] literals = imports[i]
							.split(Utils.SPLIT_AT_SEMICOLON);

					if (literals[0].startsWith("java.")) {
						throw new BundleException(
								"Explicit import of java.* packages is not permitted",
								BundleException.MANIFEST_ERROR);
					}

					if (importSet.contains(literals[0])) {
						throw new BundleException("Duplicate import "
								+ literals[0], BundleException.MANIFEST_ERROR);
					}
					importSet.add(literals[0]);

					final Tuple<HashMap<String, String>, HashMap<String, Object>> tuple = Utils
							.parseLiterals(literals, 1);
					final HashMap<String, String> dirs = tuple.getFormer();

					dirs.put(Namespace.REQUIREMENT_FILTER_DIRECTIVE, Utils
							.createFilter(PackageNamespace.PACKAGE_NAMESPACE,
									literals[0], tuple.getLatter()));

					reqs.add(new BundleRequirementImpl(revision,
							PackageNamespace.PACKAGE_NAMESPACE, dirs, null,
							Constants.IMPORT_PACKAGE + ' ' + imports[i]));
				}
			}
		}

		{
			// dynamic imports
			final String dynImportStr = mfAttrs
					.getValue(Constants.DYNAMICIMPORT_PACKAGE);
			if (dynImportStr != null) {
				final String[] dynImports = dynImportStr
						.split(Utils.SPLIT_AT_COMMA);
				for (int i = 0; i < dynImports.length; i++) {
					final String[] literals = dynImports[i]
							.split(Utils.SPLIT_AT_SEMICOLON);

					final Tuple<HashMap<String, String>, HashMap<String, Object>> tuple = Utils
							.parseLiterals(literals, 1);
					final HashMap<String, String> dirs = tuple.getFormer();

					dirs.put(Namespace.REQUIREMENT_FILTER_DIRECTIVE, Utils
							.createFilter(PackageNamespace.PACKAGE_NAMESPACE,
									literals[0], tuple.getLatter()));

					dirs.put(PackageNamespace.REQUIREMENT_RESOLUTION_DIRECTIVE,
							PackageNamespace.RESOLUTION_DYNAMIC);

					dirs.put(PackageNamespace.REQUIREMENT_EFFECTIVE_DIRECTIVE,
							PackageNamespace.EFFECTIVE_ACTIVE);

					// TODO: think of something better
					dirs.put(PackageNamespace.PACKAGE_NAMESPACE, literals[0].trim());

					if (literals[0].contains("*")) {
						dirs.put(
								PackageNamespace.REQUIREMENT_CARDINALITY_DIRECTIVE,
								PackageNamespace.CARDINALITY_MULTIPLE);
					}

					reqs.add(new BundleRequirementImpl(revision,
							PackageNamespace.PACKAGE_NAMESPACE, dirs, null,
							Constants.DYNAMICIMPORT_PACKAGE + ' '
									+ dynImports[i]));
				}
			}
		}

		{
			// package exports

			final String exportStr = mfAttrs.getValue(Constants.EXPORT_PACKAGE);
			if (exportStr != null) {
				final String[] exports = exportStr.split(Utils.SPLIT_AT_COMMA);
				for (int i = 0; i < exports.length; i++) {
					final String[] literals = exports[i]
							.split(Utils.SPLIT_AT_SEMICOLON);

					final Tuple<HashMap<String, String>, HashMap<String, Object>> tuple = Utils
							.parseLiterals(literals, 1);
					final HashMap<String, Object> attrs = tuple.getLatter();

					if (attrs
							.containsKey(Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE)
							|| attrs.containsKey(Constants.BUNDLE_VERSION_ATTRIBUTE)) {
						throw new BundleException(
								"Export statement contains illegal attributes");
					}

					final String specVer = (String) attrs
							.get(SPECIFICATION_VERSION);
					if (specVer != null) {
						final Version ver = (Version) attrs
								.get(Constants.VERSION_ATTRIBUTE);
						final Version specVerV = new Version(
								Utils.unQuote(specVer));
						if (ver != null) {
							// make sure they are identical
							if (!specVerV.equals(ver)) {
								throw new BundleException(
										"Both version and specificationVersion are given but versions are not identical");
							}
						} else {
							attrs.put(Constants.VERSION_ATTRIBUTE, specVerV);
						}
					}

					attrs.put(PackageNamespace.PACKAGE_NAMESPACE, literals[0].trim());

					if (literals[0].startsWith("java.")) {
						throw new BundleException(
								"Bundle must not export a java.* package");
					}

					attrs.put(
							PackageNamespace.CAPABILITY_BUNDLE_SYMBOLICNAME_ATTRIBUTE,
							bundleSymbolicName);
					attrs.put(
							PackageNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE,
							bundleVersion);

					caps.add(new BundleCapabilityImpl(revision,
							PackageNamespace.PACKAGE_NAMESPACE, tuple
									.getFormer(), attrs,
							Constants.EXPORT_PACKAGE + ' ' + exports[i]));
				}
			}
		}

		{
			// bundle namespace
			final String requireBundleStr = mfAttrs
					.getValue(Constants.REQUIRE_BUNDLE);

			// require bundle
			if (requireBundleStr != null) {
				final String[] requires = requireBundleStr
						.split(Utils.SPLIT_AT_COMMA);
				for (int i = 0; i < requires.length; i++) {
					final String[] literals = requires[i]
							.split(Utils.SPLIT_AT_SEMICOLON);

					final String requiredBundle = literals[0].trim();

					final Tuple<HashMap<String, String>, HashMap<String, Object>> tuple = Utils
							.parseLiterals(literals, 1);

					final Map<String, String> dirs = tuple.getFormer();

					final String visibility = tuple.getFormer().get(
							Constants.VISIBILITY_DIRECTIVE);
					if (Constants.VISIBILITY_REEXPORT.equals(visibility)) {
						dirs.put(
								BundleNamespace.REQUIREMENT_VISIBILITY_DIRECTIVE,
								BundleNamespace.VISIBILITY_REEXPORT);
					} else {
						dirs.put(
								BundleNamespace.REQUIREMENT_VISIBILITY_DIRECTIVE,
								BundleNamespace.VISIBILITY_PRIVATE);
					}

					if (Constants.RESOLUTION_OPTIONAL.equals(tuple.getFormer()
							.get(Constants.RESOLUTION_DIRECTIVE))) {
						dirs.put(
								BundleNamespace.REQUIREMENT_RESOLUTION_DIRECTIVE,
								BundleNamespace.RESOLUTION_OPTIONAL);
					}
					
					dirs.put(Namespace.REQUIREMENT_FILTER_DIRECTIVE, Utils
							.createFilter(BundleNamespace.BUNDLE_NAMESPACE,
									requiredBundle, tuple.getLatter()));

					reqs.add(new BundleRequirementImpl(revision,
							BundleNamespace.BUNDLE_NAMESPACE, dirs, null,
							Constants.REQUIRE_BUNDLE + ' ' + requires[i]));
				}
			}
		}

		return new Tuple<List<BundleCapability>, List<BundleRequirement>>(caps,
				reqs);
	}

	void addAttributeIfPresent(final Map<String, Object> attributes,
			final String name, final Attributes mfAttrs, final String header) {
		final String str = mfAttrs.getValue(header);
		if (str != null) {
			attributes.put(name, str);
		}
	}

	public static void main(String... args) throws BundleException {
		final String s1 = "AA/BB";
		final String s2 = "CDC-1.0/Foundation-1.0";
		final String s3 = "AA/BB-1.7";
		final String s4 = "JavaSE-1.6";
		final String s5 = "V1-1.5/V2-1.6";
		final String s6 = "MyEE-badVersion";
		final String s7 = "V1-1.5/V2-1.5";
		final String s8 = "EE-2.0/FF-YY";
		final String s9 = "CDC-1.0/Foundation-1.0," + "OSGi/Minimum-1.2,"
				+ "J2SE-1.4," + "JavaSE-1.6," + "AA/BB-1.7," + "V1-1.5/V2-1.6,"
				+ "MyEE-badVersion";

		final String[] reqs = new String[] { s1, s2, s3, s4, s5, s6, s7, s8, s9 };

		for (final String requiredEEStr : reqs) {
			System.out.println(requiredEEStr);
			final String s = parse(requiredEEStr);

			System.out.println(s);
			System.out.println(new BundleRequirementImpl(null, s));
			System.out.println();
		}
	}

	public static String parse(final String requiredEEStr)
			throws BundleException {
		final String[] requiredEEs = requiredEEStr.split(",");
		final StringBuffer buffer = new StringBuffer();

		if (requiredEEs.length > 1) {
			buffer.append("(|");
		}

		for (int i = 0; i < requiredEEs.length; i++) {
			final String requiredEE = requiredEEs[i].trim();
			final Matcher matcher = PARSE_EE.matcher(requiredEE);

			final String ee1;
			final String ee2;
			final String version;

			if (matcher.matches()) {
				if (matcher.group(1) != null) {
					ee1 = matcher.group(1);
					ee2 = matcher.group(3);
					version = matcher.group(2);
				} else if (matcher.group(4) != null) {
					ee1 = matcher.group(4);
					ee2 = matcher.group(5);
					version = matcher.group(6);
				} else {
					final String s = matcher.group(7);
					final int pos = s.indexOf('/');
					ee1 = pos > -1 ? s.substring(0, pos) : s;
					ee2 = pos > -1 ? s.substring(pos + 1, s.length()) : null;
					version = null;
				}
			} else {
				final Matcher matcher2 = PARSE_EE2.matcher(requiredEE);

				if (matcher2.matches()) {
					if (matcher2.group(4) == null
							|| matcher2.group(2).equals(matcher2.group(4))) {
						ee1 = matcher2.group(1);
						version = matcher2.group(2);
						ee2 = matcher2.group(3);
					} else {
						ee1 = matcher2.group(1) + "-" + matcher2.group(2);
						ee2 = matcher2.group(3) + "-" + matcher2.group(4);
						version = null;
					}
				} else {
					throw new BundleException("invalid bree string "
							+ requiredEE);
				}
			}

			if (version != null) {
				buffer.append("(&(");
			} else {
				buffer.append('(');
			}
			buffer.append(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE);
			buffer.append('=');
			buffer.append("J2SE".equals(ee1) ? "JavaSE" : ee1);
			if (ee2 != null) {
				buffer.append('/');
				buffer.append(ee2);
			}
			if (version != null) {
				buffer.append(")(version=");
				buffer.append(version);
				buffer.append("))");
			} else {
				buffer.append(')');
			}
		}

		if (requiredEEs.length > 1) {
			buffer.append(')');
		}

		return buffer.toString();
	}

	public List<BundleCapability> translateToCapability(
			final Concierge framework, final String attributeName,
			final String valueStr) {
		if (Constants.FRAMEWORK_EXECUTIONENVIRONMENT.equals(attributeName)) {
			final String[] fees = valueStr.split(Utils.SPLIT_AT_COMMA);
			final List<BundleCapability> caps = new ArrayList<BundleCapability>();

			for (final String fee : fees) {

				final String requiredEE = fee.trim();
				final Matcher matcher = PARSE_EE.matcher(requiredEE);

				final String ee1;
				final String ee2;
				final String version;

				if (matcher.matches()) {
					if (matcher.group(1) != null) {
						ee1 = matcher.group(1);
						ee2 = matcher.group(3);
						version = matcher.group(2);
					} else if (matcher.group(4) != null) {
						ee1 = matcher.group(4);
						ee2 = matcher.group(5);
						version = matcher.group(6);
					} else {
						final String s = matcher.group(7);
						final int pos = s.indexOf('/');
						ee1 = pos > -1 ? s.substring(0, pos) : s;
						ee2 = pos > -1 ? s.substring(pos + 1, s.length())
								: null;
						version = null;
					}
				} else {
					final Matcher matcher2 = PARSE_EE2.matcher(requiredEE);

					if (matcher2.matches()) {
						if (matcher2.group(4) == null
								|| matcher2.group(2).equals(matcher2.group(4))) {
							ee1 = matcher2.group(1);
							version = matcher2.group(2);
							ee2 = matcher2.group(3);
						} else {
							ee1 = matcher2.group(1) + "-" + matcher2.group(2);
							ee2 = matcher2.group(3) + "-" + matcher2.group(4);
							version = null;
						}
					} else {
						throw new IllegalStateException(
								"invalid framework execution environment "
										+ requiredEE);
					}
				}

				final Map<String, Object> attrs = new HashMap<String, Object>();

				final String eeStr = ee2 == null ? ee1 : ee1 + "/" + ee2;

				attrs.put(
						ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE,
						eeStr);
				if (version != null) {
					attrs.put(
							ExecutionEnvironmentNamespace.CAPABILITY_VERSION_ATTRIBUTE,
							new Version(version));
				}

				final BundleCapability cap = new BundleCapabilityImpl(
						framework,
						ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE,
						null, attrs,
						"ExecutionEnvironment " + eeStr + version == null ? ""
								: ' ' + version);
				caps.add(cap);
			}
			return caps;
		}
		return null;
	}
}
