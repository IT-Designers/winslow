import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {GroupApiService, GroupInfo, MemberInfo} from '../../api/group-api.service';
import {DialogService} from '../../dialog.service';

@Component({
  selector: 'app-group-details',
  templateUrl: './group-details.component.html',
  styleUrls: ['./group-details.component.css']
})
export class GroupDetailsComponent implements OnInit {

  @Input() selectedGroup: GroupInfo = null;
  @Input() myUser: MemberInfo = null;

  @Output() groupDeleteEmitter = new EventEmitter();

  constructor(private dialog: DialogService, private groupApi: GroupApiService) { }

  ngOnInit(): void {
  }

  onMemberAdded(event) {
    return this.dialog.openLoadingIndicator(
      this.groupApi.addOrUpdateMembership(this.selectedGroup.name, event)
        .then(() => {
          this.selectedGroup.members.push(event);
        }),
      'Adding Member to group'
    );
  }
  onRemoveMember(event) {
    return this.dialog.openLoadingIndicator(
      this.groupApi.deleteGroupMembership(this.selectedGroup.name, event.name),
      'Removing Member from Group'
    );
  }

  onGroupDelete() {
    this.groupDeleteEmitter.emit(this.selectedGroup);
  }
}
