# Objective

Improve the information in the summary so that it can be displayed better.

## Process

The Summary object contains a list of information about the sources, i would like to enrich that information.

### Grouping

The UI currently groups sources together, but it does it incorrectly because there is not enough information.

Add a new string value to the SourceDTO called group - this doesn't have an equivalent in source so will be null by default.

Groups are defined by the synchronize objects, there are usually one source copied to 3 other sources.  If you get all the synchronize objects it will give you the source and destination of each group.  You should update those instances with a group name that is equal to the last directory in the path of the primary source in the group.

### Additional information

Add the following attributes to the synchronize object:

+ Start Time
+ End Time (null while running)
+ An attribute for each of the counts in SyncDataDTO

When synchronise runs it should update the start time and set the other attributes to null, when it completes update the end time and all the counts.

These attributes should be then returned on the destination source in the summary.

### Gather Status Information

Update the Source JPA and database to include two extra values - gatherStart and gatherFinished.  Start should be set when the gather for that source starts, and the end set when it completes.  These two values should then be included on the summary information.