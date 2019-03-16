const btSerial = require('bluetooth-serial-port');
const terminal = require('terminal-kit').terminal;
const fs = require('fs');

const bt = new btSerial.BluetoothSerialPort();

var lastConnection = {
    address : 0,
    channel : 0
}

var activities = ['walking', 'sitting', 'running','jumping'];
var placement = ['right-pocket','left-pocket','right-hand','left-hand','left-back-pocket','right-back-pocket'];

var frequency = 1;
var dataBuffer = "";
var showDataOnScreen = true;
var commandMode = false;
var write = false;

var writeStream = undefined;

main();

var commands = {
    setFrequency : 'set-freq',
    startWrite : 'start-write',
    stopWrite : 'stop-write'
}

var commandsList = [commands.setFrequency, commands.startWrite, commands.stopWrite];
var hist = [''];


function main(){

    start();
    
    function terminate(){
        terminal.brightBlack( 'About to exit...\n' ) ;
        terminal.grabInput( false ) ;
        terminal.applicationKeypad( false ) ;
        terminal.beep() ;
        terminal.fullscreen( false ) ;
        
        // Add a 100ms delay, so the terminal will be ready when the process effectively exit, preventing bad escape sequences drop
        setTimeout( function() { process.exit() ; } , 100 ) ;
    }
    
    terminal.on('key', (name, matches, data) => {
    
        if(matches.indexOf('CTRL_C') >= 0){
            terminate();
        }

        if(matches.indexOf('CTRL_R') >= 0){
            terminal.brightBlack( 'Restarting\n' );
            end();
        }

        if(matches.indexOf('ESCAPE') >= 0){
            commandMode = !commandMode;
            if(commandMode){
                showDataOnScreen = false;
                command();
            }else{
                showDataOnScreen = true;
            }
        }
    
    });

}

function command(){
    showDataOnScreen = false;

    terminal('Enter Command : ');
    terminal.inputField({history : hist, autoComplete : commandsList, autoCompleteMenu : false}, (err , input) => {
        if(err) {
            console.log(err);
        } else {
            execCommand(input);
        }
    });
}

function execCommand(command){
    var c = command.split(' ');
    var op = c[0];
    
    switch(op){
        case commands.setFrequency:
            setFrequency(c[1]);
            break;
        case commands.startWrite:
            startWrite();
            break;
        case commands.stopWrite:
            stopWrite();
            break;
    }

}

function startWrite(){

    terminal("Give the Activity params [Activity Intensity Height Weight Placement Device ActorName] : ");
    terminal.inputField({history : hist, autoComplete : activities, autoCompleteMenu : false},  (err, input) => {
        if(err){
            console.log(err);
        } else {
            var params = input.split(' ');
            
            var activity = params[0];
            var intensity = params[1];
            var height = params[2];
            var weight = params[3];
            var placement = params[4];
            var device = params[5];
            var actorName = params[6];

            
            var metaData = {
                freq : frequency,
                activity : activity,
                intensity : intensity,
                height : height,
                weight : weight,
                placement : placement,
                actorName : actorName,
                distance : 0,
                device : device
            }

            writeData(metaData);

        }
    });
    
}

function writeData(metaData){
    var folder = 'data/'
    var fileName = folder + metaData.activity + metaData.actorName + '.txt';
    var metaFileName = folder + 'meta-' + metaData.activity + metaData.actorName + '.txt';

    //write meta data
    fs.writeFile(metaFileName,JSON.stringify(metaData),(err) => {
        if(err){
            console.log(err);
        }
    });

    writeStream = fs.createWriteStream(fileName);
    write = true;
}

function stopWrite(){
    write = false;
    writeStream.end();
}

function setFrequency(freq){
    
    var commandString = 'C SF ' + freq;

    bt.write(Buffer.from(commandString,'utf-8'), (err,bytesWritten) => {
        if(err) {
            console.log(err);
        } else {
            frequency = freq;
            console.log(bytesWritten);
        }
    });
}

bt.on('data', data => {
    var dataStr = data.toString('utf-8');
    
    if(write){
        writeStream.write(dataStr);
    }

    if(showDataOnScreen){
        console.log(dataStr);
    } else if(dataStr.split[0] == "RES"){
        terminal.brightBlue(dataStr.slice(4));
    }
});

function start(){

    bt.listPairedDevices(devices => {

        var devicesInfo = [];
        devices.forEach(element => {
            devicesInfo.push(element.name + ' [' + element.address + ']');
        });
    
        terminal.cyan('Select the phone to connect to...');
        terminal.singleColumnMenu(devicesInfo, (err,res) => {
            if(err){
                console.log(err);
                return;
            }
    
            var selectedDevice = devices[res.selectedIndex];
    
            var deviceName = selectedDevice.name;
            var deviceAddress = selectedDevice.address;
            var channel = 0;
            
    
            var services = [];
            selectedDevice.services.forEach(element => {
                services.push(element.name);
            });
    
            terminal.cyan('\nChoose a service from ').green(deviceName);
            terminal.singleColumnMenu(services, (e,r) => {
                if(err){
                    console.log(e);
                    return;
                }
    
                serviceChannel = selectedDevice.services[r.selectedIndex].channel;
    
                terminal.cyan('Connecting to ')
                    .red(selectedDevice.services[r.selectedIndex].name)
                    .cyan(' on ')
                    .green(deviceName + '[' + deviceAddress + ']');
    
                connectToDevice(deviceAddress,serviceChannel);
    
            });
        
        });
    
    
        
    });
    
    function connectToDevice(deviceAddress,channel){
    
            bt.connect(deviceAddress,channel, function(){
                //do stuff here
                console.log('\nconnected');
                lastConnection.address = deviceAddress;
                lastConnection.channel = channel;

            },function(){
                //could not connect
                console.log('could not connect');
                process.exit();
            });
    
    }

}

