const argv = require('minimist')(process.argv.slice(2));

const recordsCnt = argv['recordsCnt'];
if (!recordsCnt)
  throw new Error('Please specify the number of records that will be generated: --recordsCnt');

for(let i = 0; i < recordsCnt; i++) {
  const licenceStartDate = randomDateGreaterThanNow();
  const licenceEndDate = randomDateGreaterThan(licenceStartDate);
  console.log(`${vrn()},${toISODate(licenceStartDate)},${toISODate(licenceEndDate)},${randomVehicleType()},${randomLicensingAuthorityName()},${randomLicensePlateNumber()},${randomWheelchairAccessibleVehicle()}`);
}

function vrn() {
  return `${randomUppercaseString(2)}${randomDigit()}${randomDigit()}${randomUppercaseString(3)}`
}

function randomDateGreaterThanNow() {
  return randomDateGreaterThan(new Date());
}

function randomDateGreaterThan(start) {
  const end = Math.random()*24*60*60*1000 * 3*30;
  const date = new Date(start.getTime() + end);
  return date;
}

function toISODate(input) {
  return input.toISOString().substring(0, 10);
}

function randomVehicleType() {
  return randomDigit() % 2 == 0 ? 'taxi' : 'PHV';
}

function randomLicensePlateNumber() {
  return randomString(5);
}

function randomWheelchairAccessibleVehicle() {
  return randomDigit() % 2 ? "true" : "false";
}

function randomLicensingAuthorityName() {
  const LA = ["la-1", "la-2", "la-3"];
  return LA[ randomIntInclusive(0, LA.length - 1)  ];
}

function randomString(length) {
  const chars = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ';
  var result = '';
  for (var i = length; i > 0; --i) result += chars[Math.floor(Math.random() * chars.length)];
  return result;
}

function randomUppercaseString(length) {
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';
  var result = '';
  for (var i = length; i > 0; --i) result += chars[Math.floor(Math.random() * chars.length)];
  return result;
}

function randomDigit() {
  return Math.floor(Math.random() * 10) ;
}

function randomIntInclusive(min, max) {
  min = Math.ceil(min);
  max = Math.floor(max);
  return Math.floor(Math.random() * (max - min + 1)) + min; //The maximum is inclusive and the minimum is inclusive 
}

