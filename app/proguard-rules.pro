# Gson serialises and deserialises the models below through reflection. R8 runs
# in full mode, where `-keepattributes Signature` is only honoured for members
# matched by a keep rule, so without these rules the generic `List<T>` element
# type is stripped and Gson parses the elements as LinkedTreeMap instead of the
# real type (a ClassCastException at the first cast). Keeping the fields also
# pins the JSON keys, so data written by one build stays readable by the next.
-keep class org.btcmap.db.table.user.User { *; }
-keep class org.btcmap.db.table.user.SavedItem { *; }
-keep class org.btcmap.offline.OfflineRegionMetadata { *; }
-keep class org.btcmap.offline.OfflineRegionMetadataJson { *; }
