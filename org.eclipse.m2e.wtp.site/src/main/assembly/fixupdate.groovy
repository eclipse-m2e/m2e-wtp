def basedir = project.basedir.canonicalPath 

def ant = new AntBuilder();   // create an antbuilder
def contentJar = basedir  + "/target/site/content.jar"
def contentDir = basedir  + "/target/content.jar/"
println 'unzipping content.jar'
ant.unzip(  src: contentJar, dest:contentDir,  overwrite:"true")
println ' Doing some black magic'

File contentXml =  new File(contentDir, "content.xml")
def root = new XmlParser().parseText(contentXml.text)

def feature = root.units.unit.find{ it.@id=="org.eclipse.m2e.wtp.feature.feature.group"}
def version = feature.@version 
println "found " + feature.@id + '-' + version 

def matchDefinition = "providedCapabilities.exists(pc | pc.namespace == 'org.eclipse.equinox.p2.iu' && (pc.name == 'org.maven.ide.wtp.feature.feature.group' || pc.name == 'org.eclipse.m2e.wtp.feature.feature.group' && pc.version < '${version}'))"
println matchDefinition 

feature.update.@match = matchDefinition
feature.update[0].attributes().remove('id')
feature.update[0].attributes().remove('range')

def writer = new StringWriter()
new XmlNodePrinter(new PrintWriter(writer)).print(root)
contentXml.write(writer.toString())

println 'zipping customized content.jar'
ant.zip(  destFile: contentJar, baseDir:contentDir)
