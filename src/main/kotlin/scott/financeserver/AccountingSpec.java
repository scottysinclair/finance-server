package scott.financeserver;

import static scott.barleydb.api.specification.CoreSpec.mandatoryRefersTo;
import static scott.barleydb.api.specification.CoreSpec.ownsMany;
import static scott.barleydb.api.specification.CoreSpec.uniqueConstraint;

import scott.barleydb.api.core.types.JavaType;
import scott.barleydb.api.core.types.JdbcType;
import scott.barleydb.api.core.types.Nullable;
import scott.barleydb.api.specification.DefinitionsSpec;
import scott.barleydb.api.specification.EntitySpec;
import scott.barleydb.api.specification.JoinTypeSpec;
import scott.barleydb.api.specification.KeyGenSpec;
import scott.barleydb.api.specification.NodeSpec;
import scott.barleydb.api.specification.RelationSpec;
import scott.barleydb.api.specification.SuppressionSpec;
import scott.barleydb.api.specification.constraint.UniqueConstraintSpec;
import scott.barleydb.bootstrap.GenerateModels;
import scott.barleydb.build.specification.staticspec.Entity;
import scott.barleydb.build.specification.staticspec.StaticDefinitions;

public class AccountingSpec extends StaticDefinitions {

    /**
     * generate the query and model classes for the spec.
     */
    public static void main(String args[]) {
        GenerateModels.execute(AccountingSpec.class);
    }



    public AccountingSpec() {
        super("scott.financeserver.data");
    }

    @Override
    public void postProcess(DefinitionsSpec definitionsSpec) {
        super.postProcess(definitionsSpec);
        setAllNamesToUpperCase(definitionsSpec);
    }

    @Entity("SS_ACCOUNT")
    public static class Account {
        public static final NodeSpec id = uuidPrimaryKey();

        public static final NodeSpec name = name();

        public static final UniqueConstraintSpec uniqueName = uniqueConstraint(name);
    }

    @Entity("SS_TRANSACTION")
    public static class Transaction {
        public static final NodeSpec id = uuidPrimaryKey();

        public static final NodeSpec content = mandatoryVarchar(8000);

        public static final NodeSpec contentHash = mandatoryVarchar(40);

        public static final NodeSpec account = mandatoryRefersTo(Account.class);

        public static final NodeSpec date = mandatoryTimestamp();

        public static final NodeSpec category = mandatoryRefersTo(Category.class);

        public static final NodeSpec userCategorized = mandatoryBoolean();

        public static final NodeSpec amount = mandatoryDecimal(9, 2);

        public static final NodeSpec comment = optionalVarchar(150);

        public static final NodeSpec important = mandatoryBoolean();

        public static final UniqueConstraintSpec uniqueContentHash = uniqueConstraint(contentHash);
    }

    @Entity("SS_CATEGORY")
    public static class Category {
        public static final NodeSpec id = uuidPrimaryKey();

        public static final NodeSpec name = mandatoryVarchar(100);

        public static final NodeSpec matchers = ownsMany(CategoryMatcher.class, CategoryMatcher.category);
    }

    @Entity("SS_CATEGORY_MATCHER")
    public static class CategoryMatcher {
        public static final NodeSpec id = uuidPrimaryKey();

        public static final NodeSpec category = mandatoryRefersTo(Category.class);

        public static final NodeSpec pattern = mandatoryVarchar(200);
    }

        @Entity("SS_MONTH")
    public static class EndOfMonthStatement {
        public static final NodeSpec id = uuidPrimaryKey();

        public static final NodeSpec account = mandatoryRefersTo(Account.class);

        public static final NodeSpec year = mandatoryIntValue();

        public static final NodeSpec month = mandatoryIntValue();

        public static final NodeSpec amount = mandatoryDecimal(9, 2);
    }

    @Override
    public JoinTypeSpec getJoinType(EntitySpec entitySpec, RelationSpec relationSpec) {
        return JoinTypeSpec.LEFT_OUTER_JOIN;
    }

    @Override
    protected String createForeignKeyColumnNameForEntitySpec(EntitySpec entitySpec) {
        //parent class does table name + "id"
        String fkColumnName = super.createForeignKeyColumnNameForEntitySpec(entitySpec);
        //we remove the module name which we know is there for a morpheus table
        return removePrefix(fkColumnName);
    }

    private String removePrefix(String value) {
        int i = value.indexOf('_');
        return value.substring(i+1);
    }

    public static NodeSpec optionalInteger(String columnName) {
        NodeSpec spec = mandatoryLongValue();
        spec.setJavaType(JavaType.INTEGER);
        spec.setJdbcType(JdbcType.INT);
        spec.setColumnName(columnName);
        spec.setKeyGenSpec(KeyGenSpec.FRAMEWORK);
        spec.setNullable(Nullable.NULL);
        return spec;
    }

    public static NodeSpec uuidPrimaryKey() {
        NodeSpec spec = mandatoryUuidValue(null);
        spec.setKeyGenSpec(KeyGenSpec.FRAMEWORK);
        spec.setColumnName("ID");
        spec.setPrimaryKey(true);
        spec.setSuppression(SuppressionSpec.GENERATED_CODE_SETTER);
        return spec;
    }

    public static NodeSpec name() {
        return mandatoryVarchar50();
    }

    public static NodeSpec mandatoryVarchar50() {
        return varchar(null, 50, Nullable.NOT_NULL);
    }

    public static NodeSpec varchar(String columnName, int length, Nullable nullable) {
        NodeSpec spec = new NodeSpec();
        spec.setJavaType(JavaType.STRING);
        spec.setColumnName( columnName );
        spec.setJdbcType(JdbcType.VARCHAR);
        spec.setLength(length);
        spec.setNullable(nullable);
        return spec;
    }

    public static NodeSpec mandatoryLongValue() {
        return mandatoryLongValue(null);
    }

    public static NodeSpec mandatoryUuidValue(String columnName) {
        NodeSpec spec = new NodeSpec();
        spec.setColumnName(columnName);
        spec.setJavaType(JavaType.UUID);
        spec.setJdbcType(JdbcType.VARCHAR);
        spec.setLength(36);
        spec.setNullable(Nullable.NOT_NULL);
        return spec;
    }

    public static NodeSpec mandatoryIntValue() {
        return mandatoryIntValue(null);
    }
    public static NodeSpec mandatoryIntValue(String columnName) {
        NodeSpec spec = new NodeSpec();
        spec.setColumnName(columnName);
        spec.setJavaType(JavaType.INTEGER);
        spec.setJdbcType(JdbcType.INT);
        spec.setNullable(Nullable.NOT_NULL);
        return spec;
    }

    public static NodeSpec mandatoryLongValue(String columnName) {
        NodeSpec spec = new NodeSpec();
        spec.setColumnName(columnName);
        spec.setJavaType(JavaType.LONG);
        spec.setJdbcType(JdbcType.BIGINT);
        spec.setNullable(Nullable.NOT_NULL);
        return spec;
    }

    public static NodeSpec mandatoryTimestamp() {
        NodeSpec spec = new NodeSpec();
        spec.setJavaType(JavaType.UTIL_DATE);
        spec.setJdbcType(JdbcType.TIMESTAMP);
        spec.setNullable(Nullable.NOT_NULL);
        return spec;
    }

    public static NodeSpec mandatoryDate() {
        NodeSpec spec = new NodeSpec();
        spec.setJavaType(JavaType.UTIL_DATE);
        spec.setJdbcType(JdbcType.DATE);
        spec.setNullable(Nullable.NOT_NULL);
        return spec;
    }

    public static NodeSpec mandatoryDecimal(Integer precision, Integer scale) {
        return mandatoryDecimal(null, precision, scale);
    }

    public static NodeSpec mandatoryDecimal(String columnName, Integer precision, Integer scale) {
        NodeSpec spec = new NodeSpec();
        spec.setJavaType(JavaType.BIGDECIMAL);
        spec.setJdbcType(JdbcType.DECIMAL);
        spec.setPrecision(precision);
        spec.setScale(scale);
        spec.setColumnName( columnName );
        spec.setNullable(Nullable.NOT_NULL);
        return spec;
    }

    public static NodeSpec optionalVarchar(int length) {
        return varchar(null, length, Nullable.NULL);
    }

    public static NodeSpec mandatoryVarchar(int length) {
        return varchar(null, length, Nullable.NOT_NULL);
    }

    public static NodeSpec mandatoryBoolean() {
        NodeSpec spec = new NodeSpec();
        spec.setJavaType(JavaType.BOOLEAN);
        spec.setJdbcType(JdbcType.INT);
        spec.setNullable(Nullable.NOT_NULL);
        return spec;
    }

}
