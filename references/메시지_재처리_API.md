# 메시지 재처리 SAP OData API 조사 결과

## DataStore

### DataStore 전체 검색

- 현재 테넌트의 모든 DataStore 정보를 조회.
- 활용 방향
  - 실패한 메시지의 Body를 가져올 때 사용
  - 응답 활용
    - /feed/entry/id
      - 해당 url에 /$value 를 합친 API를 호출하여 DataStore의 메시지 중 우리가 찾고자 하는 MessageId와 일치하는 메시지의 파일을 다운로드 받을 수 있음.
      - .zip 파일로 받게 됨
      - zip 파일 아래 body 파일이 요청 페이로드
    - /feed/entry/m:properties/d:MessageId
      - 해당 값을 활용해 일치하는 MessageId 찾기
- API 경로: GET /api/v1/DataStoreEntries
- API 예시: https://{host}/api/v1/DataStoreEntries
- 응답 예시

```xml
<?xml version='1.0' encoding='utf-8'?>
<feed xmlns="http://www.w3.org/2005/Atom" xmlns:m="http://schemas.microsoft.com/ado/2007/08/dataservices/metadata" xmlns:d="http://schemas.microsoft.com/ado/2007/08/dataservices" xml:base="https://nanoh2o-is-dev.it-cpi015.cfapps.ap12.hana.ondemand.com:443/api/v1/">
    <id>https://nanoh2o-is-dev.it-cpi015.cfapps.ap12.hana.ondemand.com:443/api/v1/DataStoreEntries</id>
    <title type="text">DataStoreEntries</title>
    <updated>2026-08-19T07:58:49.29Z</updated>
    <author>
        <name/>
    </author>
    <link href="DataStoreEntries" rel="self" title="DataStoreEntries"/>
    <entry>
        <id>https://nanoh2o-is-dev.it-cpi015.cfapps.ap12.hana.ondemand.com:443/api/v1/DataStoreEntries(Id='MM2103_CPIX_ERP_2026-08-12%2016%3A34%3A50_Sender_AGp8IhqA7lR9Sm0BmtgWfUvJu47C',DataStoreName='MM2103_CPIX_ERP',IntegrationFlow='MM2103_CPIX_ERP',Type='')</id>
        <title type="text">DataStoreEntries</title>
        <updated>2026-08-19T07:58:49.291Z</updated>
        <category term="com.sap.hci.api.DataStoreEntry" scheme="http://schemas.microsoft.com/ado/2007/08/dataservices/scheme"/>
        <link href="DataStoreEntries(Id='MM2103_CPIX_ERP_2026-08-12%2016%3A34%3A50_Sender_AGp8IhqA7lR9Sm0BmtgWfUvJu47C',DataStoreName='MM2103_CPIX_ERP',IntegrationFlow='MM2103_CPIX_ERP',Type='')" rel="edit" title="DataStoreEntry"/>
        <link href="DataStoreEntries(Id='MM2103_CPIX_ERP_2026-08-12%2016%3A34%3A50_Sender_AGp8IhqA7lR9Sm0BmtgWfUvJu47C',DataStoreName='MM2103_CPIX_ERP',IntegrationFlow='MM2103_CPIX_ERP',Type='')/$value" rel="edit-media" type="application/octet-stream"/>
        <content type="application/octet-stream" src="DataStoreEntries(Id='MM2103_CPIX_ERP_2026-08-12%2016%3A34%3A50_Sender_AGp8IhqA7lR9Sm0BmtgWfUvJu47C',DataStoreName='MM2103_CPIX_ERP',IntegrationFlow='MM2103_CPIX_ERP',Type='')/$value"/>
        <m:properties>
            <d:Id>MM2103_CPIX_ERP_2026-08-12 16:34:50_Sender_AGp8IhqA7lR9Sm0BmtgWfUvJu47C</d:Id>
            <d:DataStoreName>MM2103_CPIX_ERP</d:DataStoreName>
            <d:IntegrationFlow>MM2103_CPIX_ERP</d:IntegrationFlow>
            <d:Type></d:Type>
            <d:Status>Overdue</d:Status>
            <d:MessageId>AGp8IhqA7lR9Sm0BmtgWfUvJu47C</d:MessageId>
            <d:DueAt>2026-08-14T07:34:50.839</d:DueAt>
            <d:CreatedAt>2026-08-12T07:34:50.839</d:CreatedAt>
            <d:RetainUntil>2026-08-19T07:34:50.839</d:RetainUntil>
        </m:properties>
    </entry>
    <entry>
        <id>https://nanoh2o-is-dev.it-cpi015.cfapps.ap12.hana.ondemand.com:443/api/v1/DataStoreEntries(Id='MM2103_CPIX_ERP_2026-08-12%2016%3A34%3A50_Receiver_AGp8IhqA7lR9Sm0BmtgWfUvJu47C',DataStoreName='MM2103_CPIX_ERP',IntegrationFlow='MM2103_CPIX_ERP',Type='')</id>
        <title type="text">DataStoreEntries</title>
        <updated>2026-08-19T07:58:49.291Z</updated>
        <category term="com.sap.hci.api.DataStoreEntry" scheme="http://schemas.microsoft.com/ado/2007/08/dataservices/scheme"/>
        <link href="DataStoreEntries(Id='MM2103_CPIX_ERP_2026-08-12%2016%3A34%3A50_Receiver_AGp8IhqA7lR9Sm0BmtgWfUvJu47C',DataStoreName='MM2103_CPIX_ERP',IntegrationFlow='MM2103_CPIX_ERP',Type='')" rel="edit" title="DataStoreEntry"/>
        <link href="DataStoreEntries(Id='MM2103_CPIX_ERP_2026-08-12%2016%3A34%3A50_Receiver_AGp8IhqA7lR9Sm0BmtgWfUvJu47C',DataStoreName='MM2103_CPIX_ERP',IntegrationFlow='MM2103_CPIX_ERP',Type='')/$value" rel="edit-media" type="application/octet-stream"/>
        <content type="application/octet-stream" src="DataStoreEntries(Id='MM2103_CPIX_ERP_2026-08-12%2016%3A34%3A50_Receiver_AGp8IhqA7lR9Sm0BmtgWfUvJu47C',DataStoreName='MM2103_CPIX_ERP',IntegrationFlow='MM2103_CPIX_ERP',Type='')/$value"/>
        <m:properties>
            <d:Id>MM2103_CPIX_ERP_2026-08-12 16:34:50_Receiver_AGp8IhqA7lR9Sm0BmtgWfUvJu47C</d:Id>
            <d:DataStoreName>MM2103_CPIX_ERP</d:DataStoreName>
            <d:IntegrationFlow>MM2103_CPIX_ERP</d:IntegrationFlow>
            <d:Type></d:Type>
            <d:Status>Overdue</d:Status>
            <d:MessageId>AGp8IhqA7lR9Sm0BmtgWfUvJu47C</d:MessageId>
            <d:DueAt>2026-08-14T07:34:52.163</d:DueAt>
            <d:CreatedAt>2026-08-12T07:34:52.163</d:CreatedAt>
            <d:RetainUntil>2026-08-19T07:34:52.163</d:RetainUntil>
        </m:properties>
    </entry>
    <link href="DataStoreEntries?$skiptoken=430780" rel="next"/>
</feed>
```

## JMS
