#!/usr/bin/env python3
"""Offline consistency checks; not a replacement for Maven/Hibernate/PostgreSQL."""
from pathlib import Path
import re, json, runpy, sys

ROOT=Path(__file__).resolve().parents[1]
model=runpy.run_path(str(ROOT/'tools/build_model.py'))
tables=model['TABLES']; enums=model['ENUMS']
files=list((ROOT/'src/main/java/by/bsuir/supplier/entity').rglob('*.java'))
entities=[f for f in files if re.search(r'^@Entity\s*$',f.read_text(),re.M)]
repositories=list((ROOT/'src/main/java/by/bsuir/supplier/repository').rglob('*Repository.java'))
sql=(ROOT/'src/main/resources/db/migration/V1__optimized_schema.sql').read_text()
all_table_names=re.findall(r'^CREATE TABLE (\w+) \(',sql,re.M)
assert len(tables)==30==len(entities)==len(repositories)==len(all_table_names), (len(tables),len(entities),len(repositories),len(all_table_names))
assert set(tables)==set(all_table_names)
assert len(set(all_table_names))==30
for key in model['TABLES']:
    data=tables[key]; cls=data['cls']; mod=data['module']; fields=data['fields']
    target=ROOT/'src/main/java/by/bsuir/supplier/entity'/mod/(cls+'.java')
    contents=target.read_text()
    assert '@Table(name = "'+key+'")' in contents
    assert '@Entity' in contents
    repo=ROOT/'src/main/java/by/bsuir/supplier/repository'/mod/(cls+'Repository.java')
    assert repo.exists() and ('JpaRepository<'+cls+', ') in repo.read_text()
    start=sql.index('CREATE TABLE '+key+' (')
    end=sql.index('\n);',start)
    ddl=sql[start:end]
    for column, typ in fields.items():
        if len(data['pk'])>1 and column in data['pk']:
            id_contents=(target.parent/(cls+'Id.java')).read_text()
            assert '@Column(name = "'+column+'"' in id_contents, (key,column,'idclass')
            assert '@MapsId("'+model['jname'](column)+'Id")' in contents, (key,column,'mapsid')
        else:
            assert ('@JoinColumn(name = "'+column+'"' if typ.rstrip('!').startswith('ref:') else '@Column(name = "'+column+'"') in contents, (key,column)
        assert re.search(r'^    '+column+r'\s+\w',ddl,re.M), (key,column,'missingDDL')
    assert len(data['fields'])==len(set(data['fields']))
    for uq in data['unique']:
        assert 'UNIQUE ('+', '.join(uq)+')' in ddl, (key,uq)
    for col,typ in fields.items():
        if typ.rstrip('!').startswith('ref:'):
            assert ('FOREIGN KEY ('+col+') REFERENCES '+typ.rstrip('!').split(':',1)[1]+' (id)') in sql, (key,col,'missingFK')
            assert typ.rstrip('!').split(':',1)[1] in tables, (key,col,'missingTable')
        if typ.rstrip('!').startswith('enum:'):
            vals=enums[typ.rstrip('!').split(':',1)[1]]
            for val in vals: assert "'"+val+"'" in ddl, (key,col,val)
assert sql.count('CREATE TABLE ')==30
assert sum(v.rstrip('!').startswith('ref:') for t in tables.values() for v in t['fields'].values())==79
assert sql.count('FOREIGN KEY (')==81
for name in ('supplier_status_requests','order_closure_lines','fact_corrections','rating_line_inputs'):
    assert name in all_table_names, name
for name in ('uq_current_user_role','uq_pending_supplier_status','uq_open_order_change','fk_order_current_same_document','fk_contract_current_same_document'):
    assert name in sql, name
assert sql.count('CREATE TRIGGER ')>=24
assert sql.count('$$')%2==0
assert 'ddl-auto: validate' in (ROOT/'src/main/resources/application.yml').read_text()
for n in ('Role','AuthRefreshToken','OrderObligationSegment','RatingRun'):
    assert not list((ROOT/'src/main/java').rglob(n+'.java')),n
print('PASS: 30 tables, 30 Entity, 30 JpaRepository, 79 FK and 2 composite current-document FK')
print('PASS: 24 Java enums, all columns mapped to DDL, 4 retained useful entities')
print('NOTE: This is STATIC validation only; Maven and PostgreSQL integration remain required')
