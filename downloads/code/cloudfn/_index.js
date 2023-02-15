const google = require('googleapis');

const dfltProjectId = '....';
const jobNameBase = 'dataflow-from-cloud-fn';
const dataflowPath = 'gs://.../path/to/template';

/**
 * Obtain a google authentication token and then send an http request to the dataflow service to run
 * a specific dataflow with the input filenames.
 */
function startDataflow(file, eventId, callback) {
    google.auth.getApplicationDefault(function (err, authClient, projectId) {
        if (err) {
            console.error('Authentication with default failed');
            callback(err, "Unable to authenticate");
        }
        console.info('Authentication with default successful');

        const dataflow = google.dataflow({
            version: 'v1b3',
            auth: authClient
        });

        const inputPath = `gs://${file.bucket}/${file.name}`;

        // Oddly, param projectId is sometimes set and sometimes undefined. Better to hard-wire it..
        if (typeof projectId === 'undefined') {
            console.info('Using default projectId');
            projectId = dfltProjectId;
        }

        // two concurrent jobs with the same name are not allowed, so add the file-change eventId (which is unique)
        const jobName = `${jobNameBase}-${eventId}`;

        console.info('Instantiating dataflow template with properties: ' +
            `jobName=${jobName}; projectId=${projectId}; input=${inputPath}`);

        dataflow.projects.templates.create({
            projectId: projectId,
            resource: {
                parameters: {
                    input: inputPath,
                },
                jobName: jobName,
                gcsPath: dataflowPath
            }
        }, function (err, response) {
            if (err) {
                // object err is of type Error
                console.error("problem running dataflow template, error was: " + err);
                console.error(err);
                callback(err, "problem running dataflow template");
            } else {
                console.info("Dataflow template response: ", response);
                callback(null);
            }
        });
    });
}

/**
 * Process a file uploaded to google-storage by starting a dataflow.
 */
function runFlowFor(file, context, callback) {
    if (file.resourceState === 'not_exists') {
        console.info(`File ${file.name} deleted.`);
        callback();
    } else if (file.metageneration === '1') {
        // metageneration attribute is updated on metadata changes; on create value is 1
        console.info(`File ${file.name} uploaded.`);
        startDataflow(file, context.eventId, callback)
    } else {
        console.info(`File ${file.name} metadata updated.`);
        callback();
    }
}

/**
 * Background Cloud Function to be triggered by Cloud Storage.
 *
 * Warning: if a file is uploaded, then overwritten before this cloud-function receives the event, then
 * this code will process the later version of the file twice. This is not expected to be a problem in
 * real-world systems.
 */
exports.processFile = (event, callback) => {
    const file = event.data;
    const context = event.context;

    console.info(`  Event Type: ${context.eventType}`);
    console.info(`  Bucket: ${file.bucket}`);
    console.info(`  File: ${file.name}`);
    console.info(`  Metageneration: ${file.metageneration}`);

    if (file.name.startsWith('some/path/')) {
        console.info(`File ${file.name} being processed.`);
        runFlowFor(file, context, callback)
    } else {
        console.info(`File ${file.name} not relevant.`);
        callback();
    }
}

