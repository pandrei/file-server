const AWS = require('aws-sdk');
const s3 = new AWS.S3();
const lambda = new AWS.Lambda();

exports.handler = async (event, context) => {
    // Get list of all S3 buckets
    const { Buckets } = await s3.listBuckets().promise();

    // For each bucket, create event notification for object creation
    await Promise.all(
        Buckets.map(bucket =>
            lambda
                .addPermission({
                    Action: 'lambda:InvokeFunction',
                    FunctionName: 'my-s3-processing-function',
                    Principal: 's3.amazonaws.com',
                    StatementId: `${bucket.Name}-invoke`,
                    SourceArn: `arn:aws:s3:::${bucket.Name}`,
                })
                .promise()
                .then(() =>
                    s3
                        .putBucketNotificationConfiguration({
                            Bucket: bucket.Name,
                            NotificationConfiguration: {
                                LambdaFunctionConfigurations: [
                                    {
                                        Events: ['s3:ObjectCreated:*'],
                                        LambdaFunctionArn: 'arn:aws:lambda:us-east-1:123456789012:function:my-s3-processing-function',
                                    },
                                ],
                            },
                        })
                        .promise()
                )
        )
    );

    return {
        statusCode: 200,
        body: 'Successfully set S3 event notifications for all buckets',
    };
}