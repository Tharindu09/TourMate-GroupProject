package db.migration;

import java.sql.*;
import java.util.*;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Relax the existing coordinate check without rewriting any saved trip data. */
public class V8__allow_global_trip_coordinates extends BaseJavaMigration {
 @Override public void migrate(Context context) throws Exception {
  Connection connection=context.getConnection();
  boolean mysql=connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT).contains("mysql");
  String schema=mysql?connection.getCatalog():connection.getSchema();
  List<String> oldChecks=new ArrayList<>();
  String query="SELECT tc.CONSTRAINT_NAME, cc.CHECK_CLAUSE FROM information_schema.TABLE_CONSTRAINTS tc JOIN information_schema.CHECK_CONSTRAINTS cc ON tc.CONSTRAINT_SCHEMA=cc.CONSTRAINT_SCHEMA AND tc.CONSTRAINT_NAME=cc.CONSTRAINT_NAME WHERE LOWER(tc.TABLE_NAME)='trip_items' AND tc.CONSTRAINT_TYPE='CHECK' AND tc.CONSTRAINT_SCHEMA=?";
  try(PreparedStatement statement=connection.prepareStatement(query)) {
   statement.setString(1,schema);
   try(ResultSet rows=statement.executeQuery()) {
    while(rows.next()) {String clause=rows.getString(2).toLowerCase(Locale.ROOT);if(clause.contains("latitude")||clause.contains("longitude"))oldChecks.add(rows.getString(1));}
   }
  }
  String quote=connection.getMetaData().getIdentifierQuoteString().trim();
  try(Statement statement=connection.createStatement()) {
   for(String name:oldChecks)statement.execute("ALTER TABLE trip_items DROP "+(mysql?"CHECK ":"CONSTRAINT ")+quote+name.replace(quote,quote+quote)+quote);
   statement.execute("ALTER TABLE trip_items ADD CONSTRAINT ck_trip_item_global_coordinates CHECK ((latitude IS NULL AND longitude IS NULL) OR (latitude IS NOT NULL AND longitude IS NOT NULL AND latitude BETWEEN -90 AND 90 AND longitude BETWEEN -180 AND 180))");
  }
 }
}
