def basedir = project.basedir.canonicalPath 
def contentJar = basedir  + "/target/site/content.jar"
def contentDir = basedir  + "/target/content.jar/"

println 'Unzipping content.jar'

def ant = new AntBuilder();   // create an antbuilder
ant.unzip(  src: contentJar, dest:contentDir,  overwrite:"true")

println 'Modify content.xml to make m2e-wtp updatable from m2eclipse-wtp'

File contentXml =  new File(contentDir, "content.xml")

//Identify feature Version
def root = new XmlParser().parseText(contentXml.text)
def feature = root.units.unit.find{ it.@id=="org.eclipse.m2e.wtp.feature.feature.group"}
def version = feature.@version 
println "Found " + feature.@id + '-' + version 


//Replace update match definition
String originalStatement = "<update id='org.eclipse.m2e.wtp.feature.feature.group' range='[0.0.0,$version)' severity='0'/>"
matchDefinition = "providedCapabilities.exists(pc | pc.namespace == \"org.eclipse.equinox.p2.iu\" &amp;&amp; (pc.name == \"org.maven.ide.eclipse.wtp.feature.feature.group\" || pc.name == \"org.eclipse.m2e.wtp.feature.feature.group\" &amp;&amp; pc.version &lt; \"${version}\"))"
String newStatement = "<update match='$matchDefinition' severity='0'/>"
String newContentText = contentXml.text.replace(originalStatement, newStatement)

println 'Overwriting content.xml'
contentXml.text = newContentText

println 'Zipping back customized content.jar'
ant.zip(  destFile: contentJar, baseDir:contentDir)
