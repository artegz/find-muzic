import { FileDocument } from '../../api/dto/file-document';

export class DownloadedFile {

  file: FileDocument;

  constructor(f: FileDocument) {
    this.file = f;
  }


}
