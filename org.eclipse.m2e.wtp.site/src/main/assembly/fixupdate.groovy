def basedir = project.basedir.canonicalPath 
def repositoryDir = basedir + "/target/repository"
def contentJar = repositoryDir  + "/content.jar"

def contentDir = basedir  + "/target/content.jar/"

println 'Unzipping content.jar'

def ant = new AntBuilder();   // create an antbuilder


ant.unzip(  src: contentJar, dest:contentDir,  overwrite:"true")

println 'Modify content.xml to make m2e-wtp updateable from m2eclipse-wtp and jboss tools'

File contentXml =  new File(contentDir, "content.xml")
def root = new XmlParser().parseText(contentXml.text)

def newContentText = contentXml.text
newContentText = updateMatchDefinition(root, "org.maven.ide.eclipse.wtp", "org.eclipse.m2e.wtp", newContentText)
newContentText = updateMatchDefinition(root, "org.jboss.tools.maven.jaxrs", "org.eclipse.m2e.wtp.jaxrs", newContentText) 
newContentText = updateMatchDefinition(root, "org.jboss.tools.maven.jpa", "org.eclipse.m2e.wtp.jpa", newContentText) 
newContentText = updateMatchDefinition(root, "org.jboss.tools.maven.jsf", "org.eclipse.m2e.wtp.jsf", newContentText) 

println 'Overwriting content.xml'
contentXml.text = newContentText

println 'Zipping back customized content.jar'
ant.zip(  destFile: contentJar, baseDir:contentDir)

def updateMatchDefinition(root, oldFeature, newFeature, textContent) {
	def oldFeatureGroup = "${oldFeature}.feature.feature.group"
	def featureGroup = "${newFeature}.feature.feature.group"
	def feature = root.units.unit.find{ it.@id==featureGroup }
	if (!feature) {
		println "can't find " + featureGroup
		return textContent
	}
	def version = feature.@version 
	println "Found " + feature.@id + '-' + version 

	//Replace update match definition
	String originalStatement = "<update id='${featureGroup}' range='[0.0.0,$version)' severity='0'/>"
	matchDefinition = "providedCapabilities.exists(pc | pc.namespace == \"org.eclipse.equinox.p2.iu\" &amp;&amp; (pc.name == \"${oldFeatureGroup}\" || pc.name == \"${featureGroup}\" &amp;&amp; pc.version &lt; \"${version}\"))"
	String newStatement = "<update match='$matchDefinition' severity='0'/>"
	return textContent.replace(originalStatement, newStatement)
    
}

def zipSite = basedir  + "/target/${project.artifactId}-${project.version}.zip"

println 'Deleting old zip site'
new File(zipSite).delete()

println 'Zipping updated site'
ant.zip(  destFile: zipSite, baseDir:repositoryDir)
