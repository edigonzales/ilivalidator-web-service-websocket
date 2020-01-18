The ilivalidator custom functions (packaged in a jar) will be copied in this directory during build process.

Ilivalidator will read the content of the jar and will load the classes to the classpath and register them in ilivalidator. Guess it needs the qualified function name.

Because of this registration it is not enough just to put them in the classpath.