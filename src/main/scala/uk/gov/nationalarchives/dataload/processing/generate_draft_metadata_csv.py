s3_client = boto3.client('s3')


def read_metadata(metadata_source_bucket, metadata_key):
    s3_response_object = s3_client.get_object(Bucket=metadata_source_bucket, Key=metadata_key)
    object_content = s3_response_object['Body'].read()
    return json.loads(object_content.decode("utf-8"))


def lambda_handler(event, context):
    source_bucket = event['SourceBucket']
    key_prefix = event['KeyPrefix']
    list_object_paginator = s3_client.get_paginator('list_objects_v2')
    operation_parameters = {'Bucket': source_bucket,
                            'Prefix': key_prefix}
    page_iterator = list_object_paginator.paginate(operation_parameters)
    object_keys = []
    for page in page_iterator:
        contents = page['Contents']
        object_key = contents['Key']
        object_keys.append(object_key)
        print(f'Object Key: {object_key}')

    for object_key in object_keys:

