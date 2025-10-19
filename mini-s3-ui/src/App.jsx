import { useEffect, useState } from 'react';
import {
  createBucket,
  listBuckets,
  listObjects,
  uploadObject,
  downloadObject,
  deleteObject,
  deleteBucket
} from './api';
import "@cloudscape-design/global-styles/index.css"
import {
  AppLayout,
  BreadcrumbGroup,
  Button,
  Container,
  Header,
  Input,
  Modal,
  SpaceBetween,
  Table,
  TextContent,
  ButtonDropdown,
  FormField
} from "@cloudscape-design/components";


function App() {
  const [bucket, setBucket] = useState('');
  const [buckets, setBuckets] = useState([]);
  const [objects, setObjects] = useState([]);
  const [file, setFile] = useState(null);
  const [key, setKey] = useState('');
  const [showCreateBucketModal, setShowCreateBucketModal] = useState(false);
  const [showUploadModal, setShowUploadModal] = useState(false);


  const refresh = async () => {
    try {
      const bRes = await listBuckets();
      setBuckets(bRes.data);
      if (bucket) {
        const oRes = await listObjects(bucket);
        setObjects(oRes.data);
      } else {
        setObjects([]);
      }
    } catch (err) {
      console.error(err);
    }
  };

  useEffect(() => { refresh(); }, [bucket]);

  const handleCreateBucket = async () => {
    await createBucket(bucket);
    await refresh();
    alert('Bucket created');
  };

  const handleUpload = async () => {
    await uploadObject(bucket, key || file.name, file);
    setKey('');
    setFile(null);
    await refresh();
    alert('Uploaded');
  };

  const handleDownload = async (k) => {
    const res = await downloadObject(bucket, k);
    const url = window.URL.createObjectURL(new Blob([res.data]));
    const a = document.createElement('a');
    a.href = url;
    a.download = k.split('/').pop();
    a.click();
    window.URL.revokeObjectURL(url);
  };

  const handleDelete = async (k) => {
    if (window.confirm(`Delete ${k}?`)) {
      await deleteObject(bucket, k);
      await refresh();
      alert('Deleted');
    }
  };

  const handleDeleteBucket = async () => {
    if (!bucket) return;
    if (window.confirm(`Delete bucket ${bucket} and all its content?`)) {
      await deleteBucket(bucket);
      setBucket('');
      await refresh();
      alert('Bucket deleted');
    }
  };
  const breadcrumbItems = [
    { text: "S3", href: "#" },
        (bucket ? [{ text: bucket, href: "#" }] : [])
  ];
  return (
    <>
      <AppLayout
        navigation={
          <SpaceBetween size="l">
            <Container>
              <SpaceBetween size="m">
                <Header variant="h2">Buckets</Header>
                {buckets.map(b => (
                  <Button
                    key={b}
                    variant={bucket === b ? "primary" : "normal"}
                    onClick={() => setBucket(b)}
                    fullWidth
                  >
                    {b}
                  </Button>
                ))}
              </SpaceBetween>
            </Container>
          </SpaceBetween>
        }
        content={
          <SpaceBetween size="l">
            <BreadcrumbGroup items={breadcrumbItems} />
            
            <Container
              header={
                <Header
                  variant="h1"
                  actions={
                    <SpaceBetween direction="horizontal" size="m">
                      <Button onClick={() => setShowCreateBucketModal(true)}>
                        Create bucket
                      </Button>
                      {bucket && (
                        <SpaceBetween direction="horizontal" size="m">
                          <Button onClick={() => setShowUploadModal(true)}>
                            Upload
                          </Button>
                          <Button 
                            onClick={handleDeleteBucket}
                            variant="normal"
                            iconName="remove"
                          >
                            Delete bucket
                          </Button>
                        </SpaceBetween>
                      )}
                    </SpaceBetween>
                  }
                >
                  {bucket ? `Bucket: ${bucket}` : 'Select a bucket'}
                </Header>
              }
            >
              <Table
                columnDefinitions={[
                  {
                    id: "name",
                    header: "Name",
                    cell: item => item.key,
                    sortingField: "name"
                  },
                  {
                    id: "size",
                    header: "Size",
                    cell: item => `${item.size} bytes`,
                    sortingField: "size"
                  },
                  {
                    id: "lastModified",
                    header: "Last modified",
                    cell: item => new Date(item.uploadedAt).toLocaleString(),
                    sortingField: "lastModified"
                  },
                  {
                    id: "actions",
                    header: "Actions",
                    cell: item => (
                      <ButtonDropdown
                        variant="normal"
                        items={[
                          { 
                            text: "Copy object URL",
                            iconName: "share",
                            onClick: () => {
                              const url = `${window.location.origin}/api/buckets/${bucket}/objects/${item.key}`;
                              navigator.clipboard.writeText(url);
                            }
                          },
                          { 
                            text: "Download",
                            iconName: "download",
                            onClick: () => handleDownload(item.key)
                          },
                          { type: "divider" },
                          {
                            text: "Copy",
                            iconName: "copy",
                            disabled: true // To be implemented
                          },
                          {
                            text: "Move",
                            iconName: "arrow-right",
                            disabled: true // To be implemented
                          },
                          {
                            text: "Rename",
                            iconName: "edit",
                            disabled: true // To be implemented
                          },
                          { type: "divider" },
                          {
                            text: "Delete",
                            iconName: "remove",
                            variant: "normal",
                            onClick: () => handleDelete(item.key)
                          }
                        ]}
                        expandableGroups
                        expandToViewport
                      >
                        Actions
                      </ButtonDropdown>
                    )
                  }
                ]}
                items={objects}
                loading={false}
                loadingText="Loading objects"
                selectionType="multi"
                trackBy="key"
                empty={
                  <TextContent>
                    <h4>No objects</h4>
                    <p>This bucket has no objects.</p>
                  </TextContent>
                }
              />
            </Container>
          </SpaceBetween>
        }
      />

      <Modal
        visible={showCreateBucketModal}
        onDismiss={() => setShowCreateBucketModal(false)}
        header="Create bucket"
        footer={
          <SpaceBetween direction="horizontal" size="xs">
            <Button onClick={() => setShowCreateBucketModal(false)} variant="link">
              Cancel
            </Button>
            <Button onClick={() => {
              handleCreateBucket();
              setShowCreateBucketModal(false);
            }} variant="primary">
              Create bucket
            </Button>
          </SpaceBetween>
        }
      >
        <FormField label="Bucket name">
          <Input
            value={bucket}
            onChange={e => setBucket(e.detail.value)}
            placeholder="Enter bucket name"
          />
        </FormField>
      </Modal>

      <Modal
        visible={showUploadModal}
        onDismiss={() => setShowUploadModal(false)}
        header="Upload object"
        footer={
          <SpaceBetween direction="horizontal" size="xs">
            <Button onClick={() => setShowUploadModal(false)} variant="link">
              Cancel
            </Button>
            <Button
              onClick={() => {
                handleUpload();
                setShowUploadModal(false);
              }}
              variant="primary"
              disabled={!file}
            >
              Upload
            </Button>
          </SpaceBetween>
        }
      >
        <SpaceBetween size="m">
          <FormField label="Object key (optional)">
            <Input
              value={key}
              onChange={e => setKey(e.detail.value)}
              placeholder="Enter object key"
            />
          </FormField>
          <FormField label="File">
            <input
              type="file"
              onChange={e => setFile(e.target.files[0])}
              style={{ width: '100%' }}
            />
          </FormField>
        </SpaceBetween>
      </Modal>
    </>
  );
}


/*   
#this is the MVP basic design
  return (
    <div style={{ padding: 16, fontFamily: 'sans-serif' }}>
      <h2>Mini S3 Clone</h2>

      <section>
        <h3>Create Bucket</h3>
        <input placeholder="Bucket name" value={bucket} onChange={e => setBucket(e.target.value)} />
        <button onClick={handleCreateBucket} disabled={!bucket}>Create Bucket</button>
      </section>

      <section style={{ marginTop: 24 }}>
        <h3>Upload File</h3>
        <input placeholder="Object key (optional)" value={key} onChange={e => setKey(e.target.value)} />
        <input type="file" onChange={e => setFile(e.target.files[0])} />
        <button onClick={handleUpload} disabled={!bucket || !file}>Upload</button>
      </section>

      <section style={{ marginTop: 24 }}>
        <h3>Buckets</h3>
        <ul>
          {buckets.map(b => (
            <li key={b} onClick={() => setBucket(b)} style={{ cursor: 'pointer', textDecoration: bucket === b ? 'underline' : 'none' }}>
              {b}
            </li>
          ))}
        </ul>
        <button onClick={handleDeleteBucket} disabled={!bucket} style={{ marginTop: 12, color: 'red' }}>
          Delete Bucket
        </button>
      </section>

      <section style={{ marginTop: 24 }}>
        <h3>Objects in {bucket || '(select a bucket)'}</h3>
        <ul>
          {objects.map(o => (
            <li key={o.versionId + o.key}>
              {o.key} — {o.size} bytes — {new Date(o.uploadedAt).toLocaleString()}
              <button style={{ marginLeft: 8 }} onClick={() => handleDownload(o.key)}>Download</button>
              <button style={{ marginLeft: 8, color: 'red' }} onClick={() => handleDelete(o.key)}>Delete</button>
            </li>
          ))}
        </ul>
      </section>
    </div>
  );
} */

export default App;
