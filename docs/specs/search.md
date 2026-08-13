# JBR-397 - Add search endpoint to Backup

## Overview

Provide a REST endpoint to search backed-up files by one or more criteria. All criteria are optional and combined with AND. Results are returned as a paged list.

---

## Endpoint

```
POST /api/v1/backup/search
Content-Type: application/json
```

---

## Request Body

```json
{
  "filename": "IMG_*.jpg",
  "dateFrom": "2024-01-01T00:00:00",
  "dateTo":   "2024-12-31T23:59:59",
  "sizeMin":  102400,
  "sizeMax":  10485760,
  "expiryFrom": "2025-01-01T00:00:00",
  "expiryTo":   "2025-12-31T23:59:59",
  "labels": ["holiday", "family"],
  "location": {
    "south": 50.5,
    "west":  -1.8,
    "north": 51.5,
    "east":   0.2
  },
  "page":     0,
  "pageSize": 20
}
```

### Fields

| Field        | Type             | Description |
|--------------|------------------|-------------|
| `filename`   | string           | Glob-style pattern matched against the file name (e.g. `IMG_*`, `*.jpg`). Optional. |
| `dateFrom`   | ISO-8601 datetime | File date lower bound (inclusive). Optional. |
| `dateTo`     | ISO-8601 datetime | File date upper bound (inclusive). Optional. |
| `sizeMin`    | long (bytes)     | Minimum file size, inclusive. Optional. |
| `sizeMax`    | long (bytes)     | Maximum file size, inclusive. Optional. |
| `expiryFrom` | ISO-8601 datetime | Expiry date lower bound (inclusive). Optional. |
| `expiryTo`   | ISO-8601 datetime | Expiry date upper bound (inclusive). Optional. |
| `labels`     | string[]         | One or more label names; file must have ALL listed labels. Optional. |
| `location`   | object           | Bounding box in decimal degrees (WGS-84). Only files with EXIF geo-coordinates within the box are returned. Optional. |
| `page`       | int              | Zero-based page index. Defaults to `0`. |
| `pageSize`   | int              | Results per page. Defaults to `20`, max `100`. |

#### Location bounding box

Matches the Leaflet `LatLngBounds` model — the UI passes `map.getBounds()` corners directly:

| Field   | Description                        |
|---------|------------------------------------|
| `south` | Southern latitude bound (decimal)  |
| `west`  | Western longitude bound (decimal)  |
| `north` | Northern latitude bound (decimal)  |
| `east`  | Eastern longitude bound (decimal)  |

Implemented as `meta_data.lat BETWEEN south AND north AND meta_data.long BETWEEN west AND east`. Files without EXIF location data are excluded when this criterion is present.

---

## Response Body

```json
{
  "page":       0,
  "pageSize":   20,
  "totalCount": 143,
  "results": [
    {
      "id":           1042,
      "name":         "IMG_1234.jpg",
      "fullFilename": "/photos/2024/summer/IMG_1234.jpg",
      "path":         "/photos/2024/summer",
      "locationName": "Home",
      "date":         "2024-07-15T14:32:00",
      "size":         3145728,
      "expiry":       null,
      "isImage":      true,
      "isVideo":      false,
      "icon":         "fa-file-image-o",
      "md5":          "d41d8cd98f00b204e9800998ecf8427e",
      "latitude":     51.123,
      "longitude":    -0.456
    }
  ]
}
```

### Result fields

| Field          | Description |
|----------------|-------------|
| `id`           | File identifier |
| `name`         | Filename |
| `fullFilename` | Absolute path including filename |
| `path`         | Directory path |
| `locationName` | Named backup location |
| `date`         | File date from EXIF or filesystem |
| `size`         | Size in bytes |
| `expiry`       | Expiry date, or `null` |
| `isImage`      | True if classified as an image |
| `isVideo`      | True if classified as a video |
| `icon`         | FontAwesome icon class |
| `md5`          | MD5 hash, or `null` |
| `latitude`     | Decimal latitude from EXIF, or `null` |
| `longitude`    | Decimal longitude from EXIF, or `null` |

---

## Sorting

Results are sorted by `date` descending by default. No client-controlled sort in v1.

---

## Validation / Error responses

| Condition | HTTP status |
|-----------|-------------|
| `pageSize` > 100 | `400 Bad Request` |
| `dateFrom` after `dateTo` | `400 Bad Request` |
| `sizeMin` > `sizeMax` | `400 Bad Request` |
| `location.south` >= `location.north` | `400 Bad Request` |
| No criteria supplied | `400 Bad Request` — at least one criterion required |

---

## Implementation notes

- Query built using JPA `Specification` on `FileInfo` joined to `MetaData` for location criteria, and to `FileLabel`/`Label` for label criteria.
- `filename` glob converted to a SQL `LIKE` pattern (`*` → `%`, `?` → `_`).
- Pagination via Spring `PageRequest` / `JpaSpecificationExecutor.findAll(spec, pageable)`.
- `FileRepository` and `MetaDataRepository` already extend `JpaSpecificationExecutor` — no repository changes needed.
