Maven Integration for Eclipse WTP
=================================

Maven Integration for Eclipse WTP, a.k.a m2e-wtp, aims at providing a tight integration between Maven Integration for Eclipse (a.k.a m2e) and the Eclipse Web Tools Project (WTP) .

## 📥 Installation

[Install from p2 repository](https://download.eclipse.org/m2e-wtp/releases/latest)

## Details

m2eclipse-wtp provides a set of m2e connectors used for the configuration of Java EE projects in WTP. It features :

* Support for war projects : adds the Java and Dynamic Web Facets. Support war overlays and on-the-fly resource filtering
* Support for ejb projects : adds the Java and EJB Facets. Supports deployment descriptor filtering.
* Support for ear projects : adds the EAR Facet. Supports application.xml and jboss.xml file generation, resource filtering
* Support for rar projects : adds the Java and Connector Facets.
* Support for app-client projects : adds the Java and Application Client Facets. Supports deployment descriptor filtering.
* Support for jar dependency projects : adds the Java and Utility Facets.
* Support for web-fragment projects : adds the Java and Web Fragment Facets if a web-fragment.xml file is detected in the resource folders.

Note that m2e-wtp requires m2e > 1.0. You must make sure m2e's update site (http://download.eclipse.org/technology/m2e/releases/) is defined in :
Window > Preferences > Install / Update > Available Software Sites
Since m2e 1.0 is incompatible with previous versions, you may have to uninstall old m2e and m2e-wtp versions before proceeding with the installation.

[Public wiki](https://wiki.eclipse.org/M2E-WTP)

