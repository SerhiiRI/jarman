{{&package}};

import pl.atmoterm.seposs.utils.BaseData;
import ucore.BaseAuditedEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

{{#imports}}
import {{.}};
{{/imports}}

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.Date;

@Entity
@Table(name = "DAT_{{&classUPPER}}")
public class {{&class}} extends BaseAuditedEntity{

{{#fieldList}}
   @Column(name = "{{fieldNameUPPER}}")
   private {{&fieldType}} {{&fieldName}};
{{/fieldList}}
	
   public {{&class}}() {}

{{#getterSetterFieldList}}
   public {{&fieldType}} get{{&fieldNameCap}}() {return {{&fieldName}};}
   public void set{{&fieldNameCap}}({{&fieldType}} {{&fieldName}}) {this.{{&fieldName}} = {{&fieldName}};}

{{/getterSetterFieldList}}
}
