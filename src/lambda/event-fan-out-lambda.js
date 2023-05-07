const AWS = require('aws-sdk');
const s3 = new AWS.S3({ region: 'us-east-1' });
const lambda = new AWS.Lambda({ region: 'us-east-1' });

exports.handler = async (event, context) => {
    const bucketName = event.bucketName;
    const statementId = `${bucketName}-invoke`;

    try {
        // Get the policy of the Lambda function
        const response = await lambda.getPolicy({ FunctionName: 'parse-file' }).promise();
        const policy = JSON.parse(response.Policy);

        // Check if the permission already exists
        if (policy.Statement.some((statement) => statement.Sid === statementId)) {
            console.log(`Permission already exists for bucket ${bucketName}`);

            // Check if the permission has the required properties
            const permission = policy.Statement.find((statement) => statement.Sid === statementId);
            if (
                permission &&
                permission.Principal === 's3.amazonaws.com' &&
                permission.Action === 'lambda:InvokeFunction' &&
                permission.SourceArn === `arn:aws:s3:::${bucketName}`
            ) {
                return {
                    statusCode: 200,
                    body: `S3 event notifications for bucket ${bucketName} already set`,
                };
            }
        }
    } catch(err) {
        console.log("Could not find policy for lambda parse-file");
    }

    try {
        // Create event notification for object creation
        await lambda
            .addPermission({
                Action: 'lambda:InvokeFunction',
                FunctionName: 'parse-file',
                Principal: 's3.amazonaws.com',
                StatementId: statementId,
                SourceArn: `arn:aws:s3:::${bucketName}`,
            })
            .promise();

        // Set the bucket notification configuration
        await s3
            .putBucketNotificationConfiguration({
                Bucket: bucketName,
                NotificationConfiguration: {
                    LambdaFunctionConfigurations: [
                        {
                            Events: ['s3:ObjectCreated:*'],
                            LambdaFunctionArn: 'arn:aws:lambda:us-east-1:306133122663:function:parse-file',
                        },
                    ],
                },
            })
            .promise();

        return {
            statusCode: 200,
            body: `Successfully set S3 event notifications for bucket ${bucketName}`,
        };
    } catch (err) {
        console.log(`Error setting S3 event notifications for bucket ${bucketName}`, err);
        return {
            statusCode: 500,
            body: `Error setting S3 event notifications for bucket ${bucketName}: ${err}`,
        };
    }
};