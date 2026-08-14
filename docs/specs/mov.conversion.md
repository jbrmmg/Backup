# Objective (JBR-623)

Currently, MOV files are converted into MP4 files as part of the import - this presents a problem that if the MOV file is presented again it will not detect that is already been imported.

I propose that if a file is converted then the following custom meta data attributes are added and the description property in the meta data updated:

New Properties:

+ jbr_original_file - name of the original file.
+ jbr_original_file_md5 - the MD5 calculated on the original file.
+ jbr_original_file_size - the size in bytes of the original file.

Update:

+ Description - "converted from <original file> at <date/time>"

The date/time in the meta data must be preserved from the original file (see Stage 1 notes).

I would like to implement this in stages.

## Stage 1 - Update the conversion process ✅

Modify the MOV -> MP4 conversion process to:

+ Add the three custom meta data fields and the description to the converted MP4 via ffmpeg `-metadata` flags in the same conversion command — no extra process invocation needed for these. All required values (original name, MD5, size, date) are already present on `PreImportFileDTO` by the time conversion runs: `file.getFilename()`, `file.getMd5()` (from `ReadPreImportFile`), `file.getSize()` (from `ReadPreImportFile`), and `file.getImportDate()` (from `GatherMetaData`). This means `copyConvertMov()` in `FileSystem` will need to accept these values to build the command.
+ Explicitly set the EXIF date/time meta data inside the MP4 using `writeExifDate()` after conversion — do not rely solely on the ffmpeg `-movflags use_metadata_tags` flag, as this does not reliably transfer `DateTimeOriginal`/`CreateDate` from MOV to MP4.
+ Set the date/time of the file on disk (this is already done via `setFileFromLocalDateTime`).

Note: the filesystem timestamp is already being preserved correctly. Only the EXIF date inside the MP4 needs to be added.

Confirm with unit tests - verify manually - do not move to Stage 2 until given go ahead.

## Stage 2 - Additional optional meta-data ✅

As part of the reading of file information look for the custom meta-data properties - create a new optional object (including DB storage) called Custom Meta Data - this stores the three new custom properties (original file name, MD5, size).

The new table should be keyed to the `FileSystemObject` ID of the file it belongs to (i.e. a FK to the `file` table). This follows the same pattern as all other file metadata in the system.

The information should be returned with the file information in the same way as meta data is today - you can embed the custom meta data in the existing meta data like this:

File Info -> Meta Data -> Optional Meta Data

Confirm with unit tests - verify manually - do not move to Stage 3 until given go ahead.

## Stage 3 - Verify Ignore functionality (Information Only)

Code review confirms this should already work. When a MOV file is ignored, `ignoreSelectedFile` stores the original MOV's name, MD5, size, and date in the ignore table. When the MOV is re-presented, `CheckFileIgnored` matches on the original MOV's MD5 — no conversion takes place and the ignore record was created from the MOV's attributes, so the match will succeed.

There may already be an integration test covering this. Review and verify when reached — no action expected unless a gap is found.

## Stage 4 - Verify Already Imported functionality

If the MOV file is presented again it should be flagged as already imported. The current `CheckFileConfirmedImported` step checks four attributes (MD5, filename, size, date) against the converted MP4 in the library — this will not work for a re-presented MOV because the reconverted MP4 will have a different MD5.

The solution is to match against the three stored custom fields plus the preserved date:

+ `jbr_original_file_md5` matches the incoming MOV's MD5.
+ `jbr_original_file_size` matches the incoming MOV's size.
+ `jbr_original_file` matches the incoming MOV's filename.
+ Date: the library MP4's date was set from the original MOV's EXIF in Stage 1, so it will match the incoming MOV's date. No additional custom field is required.

**Important implementation note:** `CheckDuplicateFile` builds the "similar files" list by searching the `file` table by MD5. It will not find the library MP4 when given the original MOV's MD5, because the stored MD5 in the `file` table belongs to the converted MP4. Stage 4 must extend `CheckDuplicateFile` (or introduce a new step) to also search by `jbr_original_file_md5` against the custom metadata table, so that the library MP4 is included in the similar files list and `CheckFileConfirmedImported` can evaluate it.

Confirm with unit tests - verify manually.

If the system can detect a MOV file is already imported then we are all very happy.
