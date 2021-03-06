import java.text.SimpleDateFormat

/** 
 * A script that increments the build number before every compile and stores the build number within the
 * application.properties file. The property can be accessed via the app's metadata "app.version.build", e.g., 
 * in views via <g:meta name="app.version.build"> or ${grailsApplication.metadata['app.version.build']}.
 *
 * Note: the build number will not be correct if multiple persons develop and use a versioning system
 * 
 * @author: Joerg Rech
 * 
 * Based on scripts by Kevin Gill	
 * @see http://www.wthr.us/2010/07/08/adding-an-auto-incrementing-build-number-to-a-grails-application/
 * and Steve Berczuk
 * @ http://steveberczuk.blogspot.com/2011/05/displaying-build-numbers-in-grails-apps.html
 */
eventCompileStart = { kind ->
	// retrieve previous build number and increment or start with 1 (ultra-simple: will not survive versioning)
	def buildNumber = metadata.'app.version.build'
	if (!buildNumber)
		buildNumber = 1
	else
		buildNumber = Integer.valueOf(buildNumber) + 1

	// store new build number in metadata of app
	metadata.'app.version.build' = buildNumber.toString()
	
	// capture date and profile of build
	def formatter = new SimpleDateFormat("MMM dd, yyyy")
	def buildDate = formatter.format(new Date(System.currentTimeMillis()))
	metadata.'app.version.build.date' = buildDate
	metadata.'app.version.build.env' = grailsEnv
	
	// alternative build numbers
	def build = System.getProperty("build.number", "CUSTOM")

	def revision = null
	def proc
	try { // get Git build number
		proc = "git rev-parse --short HEAD".execute()
//		proc = "git shortlog | grep -E \'^[ ]+\\w+\' | wc -l".execute()
		proc.waitFor()
		revision = revision ?: proc.in.text.replace('\n', '')
//		proc = "git rev-parse --short HEAD".execute()
//		proc.waitFor()
//		metadata.'app.version.build.git' = proc.in.text.replace('\n', '').trim()
	} catch (Exception e) {	
//		println e	// Printout of Exception will only confuse (novice) users
//		println "Could not get build info from GIT! (probably not installed on system)"
//		event "StatusUpdate", ["Could not get build info from GIT! (probably not installed on system)"]
	}

	try { // get Mercurial build number
		proc = "hg id -i -n -b -t".execute()
		proc.waitFor()
		revision = revision ?: proc.in.text.replace('\n', '').trim()
	} catch (Exception e) {	
//		println e	// Printout will only confuse (novice) users
//		println "Could not get build info from Mercurial! (probably not installed on system)"
	}

	try { // get Subversion build number
		proc = "svnversion".execute()
		proc.waitFor()
		revision = revision ?: proc.in.text.replace('\n', '').trim()
	} catch (Exception e) {
//		println e	// Printout will only confuse (novice) users
//		println "Could not get build info from Subversion (SVN)! (probably not installed on system)"
	}
	
	metadata.'app.version.revision' = revision ?: "N/A"
	
	// Save / persist metadata
	metadata.persist()
}

/**
* A script that calculates some project metrics before every compile and stores the build number within the
* application.properties file. The metrics can be accessed via the app's metadata "'app.stats.'+name+'.<metric>'", e.g.,
* in views via ${grailsApplication.metadata['app.stats.Domain_Classes.loc']}
*
* Metrics:
* - Lines of Code (LOC)
* - Number of files
*
* @author: Joerg Rech
*/
eventCompileStart = { kind ->
	// Calculate the loc / stats of the grails system using "grails stats"
	def stats = []
	def cmd = null // "grails stats"
	def osName = System.getProperty("os.name").toLowerCase()
	
	// differentiate based on OS @see http://www.mkyong.com/java/how-to-detect-os-in-java-systemgetpropertyosname/
	if		(osName.contains("win"))	cmd = "cmd /c grails stats"	// works
	else if	(osName.contains("nix"))	cmd = "grails stats"		// works: checked by thomas bittermann 
	else if	(osName.contains("nux"))	cmd = "grails stats"		// same as unix
	else if	(osName.contains("sunos"))	cmd = "grails stats"		// same as unix
	// TODO: check how to call command on MacOS 
//	else if	(osName.contains("mac"))	cmd = "grails stats"		// unclear: does it work?
	
	if (!cmd) {
		event "StatusUpdate", ["Could not get stats from Grails!"]
		return	// Exit if OS is unknown
	}
	event "StatusUpdate", ["Retrieving stats about the application"]

	def out = cmd.execute().text
	out.split("\n").each { line ->
		if (line =~ /^[\s]*\|[A-Za-z\s]*\|[0-9\s]*\|[0-9\s]*.*$/) {
			def tokenSplit = line.split("\\|")
			def files	= tokenSplit[2].trim()
			def loc		= tokenSplit[3].trim()
			def name	= tokenSplit[1].trim().replace(' ', '_')
			
			def filesID = 'app.stats.'+name+'.files'
			def locID	= 'app.stats.'+name+'.loc'
			metadata.put(filesID, files)
			metadata.put(locID,	loc)
			
			stats << [tokenSplit[1].trim(), tokenSplit[2].trim(), tokenSplit[3].trim()]
		}
	}
	metadata.persist()
	
//	binding.variables.each { println it.key }
}

eventCompileEnd	= {}

/** 
 * called before "grails war"
 */
eventWarStart 	= {
	def appName			= metadata.'app.name'
	def versionNumber	= metadata.'app.version'
	def buildNumber		= metadata.'app.buildNumber'
	println "WAR packaging started on ${appName} version ${versionNumber} build ${buildNumber}"
}
eventWarEnd 	= {}
