# Route-Crafter-ArcRoutingLibrary

Fork of [Olibear/ArcRoutingLibrary: A collection of problem abstractions and solver implementations for arc-routing problems](https://github.com/Olibear/ArcRoutingLibrary)

lots of hacks and begging of AI so it can compile using TeaVM to javascript for [Route Crafter](https://github.com/seen-one/Route-Crafter).

## Compile
To compile, install JDK and Maven then run this command

    mvn clean package -Pteavm -DskipTests

## Command line usage example for testing

    node teavm-nodejs-test.js 7 test.oarlib
    
    java -jar target\arc-routing-library-1.0.0-fat.jar 7 test.oarlib
* Where the number refers to the [mode](https://github.com/Olibear/ArcRoutingLibrary/blob/master/HOW_TO_USE.txt)
* .oarlib files can be generated with Route Crafter by adding accessing the debug menu by appending ?debug to the end of the URL.
* Or open teavm-web-ui.html to test in the web browser
## Changes

* Prepared for Maven compiling
* TeaVM doesn't support most libraries, most aren't needed so they are stubbed or are replaced with something simpler
* Blossom V replaced with greedy
* mode 6 (Christofides's Directed Rural Postman heuristic) disabled due to MSArbor requirement
* Hierholzer forward movement preference
* More