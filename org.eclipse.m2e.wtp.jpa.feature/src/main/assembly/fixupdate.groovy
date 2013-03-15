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

def basedir = project.basedir.canonicalPath 
def repositoryDir = basedir + "/target/site"
def contentJar = repositoryDir  + "/content.jar"
def contentDir = basedir  + "/target/content.jar/"

def ant = new AntBuilder();   // create an antbuilder
println 'Unzipping content.jar'
ant.unzip(  src: contentJar, dest:contentDir,  overwrite:"true")

println 'Modify content.xml to make m2e-wtp jpa updateable from jboss tools'

File contentXml =  new File(contentDir, "content.xml")
def root = new XmlParser().parseText(contentXml.text)

def newContentText = contentXml.text
newContentText = updateMatchDefinition(root, "org.jboss.tools.maven.jpa", "org.eclipse.m2e.wtp.jpa", newContentText) 

println 'Overwriting content.xml'
contentXml.text = newContentText

println 'Zipping back customized content.jar'
ant.zip(  destFile: contentJar, baseDir:contentDir)

println 'Fixing broken signed feature site'

File siteFeaturesDir = new File(repositoryDir, "features")
String siteFeaturesFileName = null;
siteFeaturesDir.eachFileMatch(~/.*feature.*/) {siteFeaturesFileName = it.name } 

println "expected feature name is ${siteFeaturesFileName}"

File signedFeature = null;
new File(basedir + "/target").eachFileMatch(~/.*feature.*jar/) {signedFeature = it } 
if (signedFeature) {
  println "Moving ${signedFeature} to ${siteFeaturesFileName}"
  ant.move(  file: signedFeature.absolutePath, toFile: siteFeaturesDir.absolutePath + "/" + siteFeaturesFileName,  overwrite:"true")
}

