import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {GroupApiService, GroupInfo} from '../../api/group-api.service';
import {DialogService} from '../../dialog.service';
import {Link} from "../../api/winslow-api";

@Component({
  selector: 'app-group-details',
  templateUrl: './group-details.component.html',
  styleUrls: ['./group-details.component.css']
})
export class GroupDetailsComponent implements OnInit {

  @Input() selectedGroup?: GroupInfo;
  @Input() myUser?: Link;

  @Output() groupDeleteEmitter = new EventEmitter();

  constructor(private dialog: DialogService, private groupApi: GroupApiService) { }

  ngOnInit(): void {
  }

  onMemberAdded(user: Link) {
    const group = this.selectedGroup;
    if (group == undefined) {
      this.dialog.error("Cannot add member to group: No group selected.");
      return;
    }
    return this.dialog.openLoadingIndicator(
      this.groupApi.addOrUpdateMembership(group.name, user)
        .then(() => {
          group.members.push(user);
          this.selectedGroup = group;
        }),
      'Adding Member to group'
    );
  }
  onRemoveMember(link: Link) {
    const group = this.selectedGroup;
    if (group == undefined) {
      this.dialog.error("Cannot remove member from group: No group selected.");
      return;
    }
    return this.dialog.openLoadingIndicator(
      this.groupApi.deleteGroupMembership(group.name, link.name),
      'Removing Member from Group'
    );
  }

  onGroupDelete() {
    this.groupDeleteEmitter.emit(this.selectedGroup);
  }
}
