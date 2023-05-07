const AWS = require('aws-sdk');
const s3 = new AWS.S3();

exports.handler = async (event, context) => {
    // Retrieve the bucket and key from the event
    const bucketName = event.Records[0].s3.bucket.name;
    const key = decodeURIComponent(event.Records[0].s3.object.key.replace(/\+/g, " "));

    // Retrieve the object from S3
    const s3Object = await s3.getObject({ Bucket: bucketName, Key: key }).promise();

    // Print the contents of the file
    console.log(`Contents of file ${key}:`);
    console.log(s3Object.Body.toString('utf-8'));

    console.log(`File ${key} processed successfully`);
};