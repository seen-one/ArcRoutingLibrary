#!/usr/bin/env node

/**
 * TeaVM ArcRoutingLibrary Node.js Test Harness v2
 * 
 * This script provides a simpler interface for testing the TeaVM compilation
 * by simulating what the TeaVM runtime would do.
 * 
 * Usage:
 *   node teavm-nodejs-test.js [solver] [oarlib-file | instance-name]
 */

const fs = require('fs');
const path = require('path');
const vm = require('vm');

// Configuration
const teavmJsPath = path.join(__dirname, 'target', 'teavm', 'teavm.js');
const projectRoot = __dirname;

console.log('================================================');
console.log('TeaVM Arc Routing Library - Node.js Tester');
console.log('================================================\n');

// Validate TeaVM JavaScript exists
if (!fs.existsSync(teavmJsPath)) {
    console.error('❌ Error: TeaVM JavaScript not found');
    console.error(`   Expected at: ${teavmJsPath}`);
    console.error('\n   Please build with: mvn package -Pteavm -DskipTests\n');
    process.exit(1);
}

console.log(`✓ TeaVM JavaScript located: ${teavmJsPath}`);
console.log(`✓ File size: ${fs.statSync(teavmJsPath).size} bytes\n`);

// Parse command line arguments
const args = process.argv.slice(2);

if (args.length < 2) {
    showHelp();
    process.exit(0);
}

const solverNum = args[0];
const instanceArg = args[1];

console.log(`📋 Solver: ${solverNum}`);
console.log(`📁 Instance: ${instanceArg}\n`);

// Load OARLIB file if specified
let instanceContent = instanceArg;
if (instanceArg.endsWith('.oarlib') || instanceArg.endsWith('.txt')) {
    let filePath = instanceArg;
    
    // Try different paths
    if (!fs.existsSync(filePath)) {
        filePath = path.join(projectRoot, instanceArg);
    }
    if (!fs.existsSync(filePath)) {
        filePath = path.join(process.cwd(), instanceArg);
    }
    
    if (!fs.existsSync(filePath)) {
        console.error(`❌ Error: Could not find OARLIB file: ${instanceArg}`);
        console.error(`   Searched in:`);
        console.error(`     - ${instanceArg}`);
        console.error(`     - ${path.join(projectRoot, instanceArg)}`);
        console.error(`     - ${path.join(process.cwd(), instanceArg)}\n`);
        process.exit(1);
    }
    
    console.log(`✓ Loading OARLIB file: ${filePath}`);
    try {
        instanceContent = fs.readFileSync(filePath, 'utf8');
        const lines = instanceContent.split('\n').length;
        console.log(`✓ Loaded ${lines} lines (${instanceContent.length} bytes)\n`);
    } catch (e) {
        console.error(`❌ Error reading file: ${e.message}\n`);
        process.exit(1);
    }
}

// Create a minimal sandbox for TeaVM execution
console.log('🚀 Initializing TeaVM runtime...');

// Create an exports object that will hold the TeaVM exports
const teavmExports = {};

// Capture all output from TeaVM
let capturedOutput = '';

const sandbox = {
    console: {
        log: (...args) => {
            const output = args.join(' ');
            capturedOutput += output + '\n';
            console.log(output);
        },
        error: (...args) => {
            const output = args.join(' ');
            capturedOutput += output + '\n';
            console.error(output);
        },
        warn: (...args) => {
            const output = args.join(' ');
            capturedOutput += output + '\n';
            console.warn(output);
        },
        info: (...args) => {
            const output = args.join(' ');
            capturedOutput += output + '\n';
            console.log(output);
        },
    },
    process: {
        argv: ['node', 'teavm', solverNum, instanceContent],
        exit: process.exit,
    },
    global: {},
    setTimeout: setTimeout,
    setInterval: setInterval,
    clearTimeout: clearTimeout,
    clearInterval: clearInterval,
    exports: teavmExports,
    module: {
        exports: teavmExports
    },
    define: undefined,
};

// Add reference to self in sandbox
sandbox.global = sandbox;

console.log('✓ Sandbox created\n');

// Load and execute TeaVM code
console.log('================================================');
console.log('Execution Output:');
console.log('================================================\n');

try {
    console.log('ℹ️  Loading TeaVM JavaScript...\n');
    const teavmCode = fs.readFileSync(teavmJsPath, 'utf8');
    
    // Execute in sandbox
    vm.runInNewContext(teavmCode, sandbox, {
        timeout: 300000, // 5 minute timeout
        displayErrors: true
    });
    
    // Check if main function was exported and call it
    if (teavmExports.main) {
        console.log('🎯 Invoking main function...\n');
        
        // Call the main function with arguments and callback
        teavmExports.main([solverNum, instanceContent], (err) => {
            if (err) {
                console.error('Error from main:', err);
            }
        });
    } else {
        console.warn('⚠️  Warning: main function not found in exports');
    }
    
    console.log('\n================================================');
    console.log('✓ TeaVM Execution Completed Successfully');
    console.log('================================================\n');
    
} catch (e) {
    console.error('\n================================================');
    console.error('❌ Error During TeaVM Execution:');
    console.error('================================================');
    console.error(`Error: ${e.message}`);
    if (e.stack) {
        console.error('\nStack trace:');
        console.error(e.stack.split('\n').slice(0, 10).join('\n'));
    }
    console.error('================================================\n');
    process.exit(1);
}

function showHelp() {
    console.log(`
Usage: node teavm-nodejs-test.js [solver] [instance]

[solver] options:
  1 - Directed Chinese Postman (DCPP) - Edmonds
  2 - Undirected Chinese Postman (UCPP) - Edmonds
  3 - Mixed Chinese Postman (MCPP) - Frederickson
  4 - Mixed Chinese Postman (MCPP) - Yaoyuenyong
  5 - Windy Chinese Postman (WPP) - Win
  6 - [NOT SUPPORTED] Directed Rural Postman (DRPP)
  7 - Windy Rural Postman (WRPP) - Benavent H1

[instance] can be:
  - Path to OARLIB file (.oarlib or .txt)
  - A test instance name

Examples:
  node teavm-nodejs-test.js 7 route_crafter_graph_largest_component.oarlib
  node teavm-nodejs-test.js 5 test_instance
  node teavm-nodejs-test.js 2 ./instances/graph.oarlib

Build Instructions:
  mvn package -Pteavm -DskipTests

================================================
    `);
}
